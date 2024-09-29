package com.omarahmed42.socialmedia.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.enums.CommentStatus;
import com.omarahmed42.socialmedia.enums.PostStatus;
import com.omarahmed42.socialmedia.exception.CommentNotFoundException;
import com.omarahmed42.socialmedia.exception.ForbiddenCommentAccessException;
import com.omarahmed42.socialmedia.exception.ForbiddenPostAccessException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.PostNotFoundException;
import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.CommentAttachment;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.projection.CommentInputProjection;
import com.omarahmed42.socialmedia.repository.CommentRepository;
import com.omarahmed42.socialmedia.repository.PostRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.service.BlockingService;
import com.omarahmed42.socialmedia.service.CommentService;
import com.omarahmed42.socialmedia.service.FriendService;
import com.omarahmed42.socialmedia.specification.CommentSpecification;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private final BlockingService blockingService;
    private final FriendService friendService;

    @Override
    public Comment findComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        SecurityUtils.throwIfNotAuthenticated();

        if (isCommentOwner(comment, authenticatedUserId)) {
            return comment;
        }

        Long commentOwner = comment.getUser().getId();
        if (blockingService.isBlocked(commentOwner, authenticatedUserId)) {
            return null;
        }

        Post post = comment.getPost();
        if (post.getPostStatus() != PostStatus.PUBLISHED)
            throw new ForbiddenCommentAccessException("Forbidden: cannot access comment with id " + commentId);

        if (friendService.isFriend(authenticatedUserId, commentOwner)
                && comment.getCommentStatus() == CommentStatus.PUBLISHED) {
            return comment;
        }

        return null;
    }

    @Override
    public Comment saveComment(CommentInputProjection commentInputProjection) {
        SecurityUtils.throwIfNotAuthenticated();
        if (commentInputProjection.getPostId() == null)
            throw new InvalidInputException("Post id cannot be null");

        Post post = postRepository.findById(commentInputProjection.getPostId())
                .orElseThrow(PostNotFoundException::new);

        if (post.getPostStatus() != PostStatus.PUBLISHED)
            throw new ForbiddenPostAccessException(
                    "Forbidden: cannot access post with id " + commentInputProjection.getPostId());

        CommentStatus commentStatus = CommentStatus.valueOf(commentInputProjection.getCommentStatus());
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        if (!isPostOwner(post, authenticatedUserId)) {
            Long postOwnerId = post.getUser().getId();
            throwIfBlocked(authenticatedUserId, postOwnerId);
            throwIfNonFriend(authenticatedUserId, postOwnerId);
        }

        return switch (commentStatus) {
            case DRAFT -> saveCommentAsDraft(authenticatedUserId, commentInputProjection);
            case PUBLISHED -> publishComment(authenticatedUserId, commentInputProjection);
        };
    }

    private void throwIfNonFriend(Long friendId, Long postOwnerId) {
        boolean isFriend = friendService.isFriend(postOwnerId, friendId);
        if (!isFriend)
            throw new ForbiddenPostAccessException("Forbidden: cannot comment on this post");
    }

    private void throwIfBlocked(Long blockedId, Long postOwnerId) {
        boolean isBlocked = blockingService.isBlocked(postOwnerId, blockedId);
        if (isBlocked)
            throw new ForbiddenPostAccessException("Forbidden: cannot comment on this post");
    }

    private boolean isPostOwner(Post post, Long userId) {
        Long postOwnerId = post.getUser().getId();
        return postOwnerId.equals(userId);
    }

    private Comment saveCommentAsDraft(Long userId, CommentInputProjection commentInputProjection) {
        if (commentInputProjection.getPostId() == null)
            throw new InvalidInputException("Post id cannot be empty");

        if (commentInputProjection.getId() == null) {
            Post post = postRepository.findById(commentInputProjection.getPostId())
                    .orElseThrow(
                            () -> new PostNotFoundException(
                                    "Post not found with id " + commentInputProjection.getPostId()));
            throwIfPostNotPublic(post);
            Comment comment = new Comment();
            comment.setContent(commentInputProjection.getContent());
            comment.setUser(userRepository.getReferenceById(userId));
            comment.setPost(post);
            comment.setCommentStatus(CommentStatus.DRAFT);
            comment = commentRepository.save(comment);
            return comment;
        }

        Comment comment = commentRepository.findById(commentInputProjection.getId())
                .orElseThrow(CommentNotFoundException::new);

        if (!commentInputProjection.getPostId().equals(comment.getPost().getId()))
            throw new InvalidInputException("Invalid post id " + commentInputProjection.getPostId());

        if (comment.getCommentStatus() != CommentStatus.DRAFT)
            throw new InvalidInputException("Comment status must be draft");

        if (StringUtils.equals(comment.getContent(), commentInputProjection.getContent()))
            return comment;
        comment.setContent(commentInputProjection.getContent());
        comment = commentRepository.save(comment);
        return comment;
    }

    private Comment publishComment(Long userId, CommentInputProjection commentInputProjection) {
        if (commentInputProjection.getPostId() == null)
            throw new InvalidInputException("Post id cannot be empty");

        if (commentInputProjection.getId() == null) {
            throwIfBlankContent(commentInputProjection.getContent());
            Post post = postRepository.findById(commentInputProjection.getPostId())
                    .orElseThrow(
                            () -> new PostNotFoundException(
                                    "Post not found with id " + commentInputProjection.getPostId()));
            throwIfPostNotPublic(post);
            Comment comment = new Comment();
            comment.setContent(commentInputProjection.getContent());
            comment.setCommentStatus(CommentStatus.PUBLISHED);
            comment.setUser(userRepository.getReferenceById(userId));
            comment.setPost(post);
            comment = commentRepository.save(comment);
            return comment;
        }

        Comment comment = commentRepository.findById(commentInputProjection.getId())
                .orElseThrow(CommentNotFoundException::new);
        throwIfNotCommentOwner(comment, userId);

        if (!StringUtils.isBlank(commentInputProjection.getContent())) {
            Long originalPostId = comment.getPost().getId();
            if (!originalPostId.equals(commentInputProjection.getPostId()))
                throw new InvalidInputException("Invalid post id " + commentInputProjection.getPostId());

            throwIfPostNotPublic(comment.getPost());

            if (isNotModified(comment, commentInputProjection))
                return comment;

            comment.setContent(commentInputProjection.getContent());
            comment.setCommentStatus(CommentStatus.PUBLISHED);
            comment = commentRepository.save(comment);
            return comment;
        }

        List<CommentAttachment> commentAttachments = comment.getCommentAttachments();
        if (commentAttachments == null || commentAttachments.isEmpty()) {
            throw new InvalidInputException("Comment must at least have non-blank content or uploaded attachments");
        }

        if (isNotModified(comment, commentInputProjection))
            return comment;

        comment.setContent(commentInputProjection.getContent());
        comment.setCommentStatus(CommentStatus.PUBLISHED);
        comment = commentRepository.save(comment);
        return comment;
    }

    private void throwIfPostNotPublic(Post post) {
        if (post.getPostStatus() != PostStatus.PUBLISHED)
            throw new ForbiddenPostAccessException("Forbidden: Cannot access post with id " + post.getId());
    }

    private void throwIfBlankContent(String content) {
        if (StringUtils.isBlank(content))
            throw new InvalidInputException("Post must at least have content");
    }

    private void throwIfNotCommentOwner(Comment comment, Long userId) {
        if (!isCommentOwner(comment, userId))
            throw new ForbiddenCommentAccessException("Forbidden: Cannot access comment with id " + comment.getId());
    }

    private boolean isCommentOwner(Comment comment, Long userId) {
        Long commentOwnerId = comment.getUser().getId();
        return commentOwnerId.equals(userId);
    }

    private boolean isNotModified(Comment comment, CommentInputProjection commentInputProjection) {
        final CommentStatus oldCommentStatus = CommentStatus.valueOf(comment.getCommentStatus().toString());
        final CommentStatus newCommentStatus = CommentStatus.valueOf(commentInputProjection.getCommentStatus());
        return (oldCommentStatus == newCommentStatus
                && StringUtils.equals(comment.getContent(), commentInputProjection.getContent()));
    }

    @Override
    public Boolean deleteComment(Long id) {
        SecurityUtils.throwIfNotAuthenticated();
        Comment comment = commentRepository
                .findCommentById(id)
                .orElseThrow(CommentNotFoundException::new);
        Post post = comment.getPost();
        if (post.getPostStatus() == PostStatus.DRAFT) {
            throw new PostNotFoundException();
        } else if (post.getPostStatus() != PostStatus.PUBLISHED) {
            throw new ForbiddenPostAccessException("Forbidden: Cannot delete comment");
        }

        throwIfNotCommentOwner(comment, SecurityUtils.getAuthenticatedUserId());
        commentRepository.delete(comment);
        return true;
    }

    @Override
    public Post getPostBy(Comment comment) {
        SecurityUtils.throwIfNotAuthenticated();
        Post post = postRepository.findById(comment.getId()).orElseThrow(PostNotFoundException::new);
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        Long userId = comment.getUser().getId();

        if (isPostOwner(post, authenticatedUserId))
            return post;

        final boolean isFriend = friendService.isFriend(authenticatedUserId, userId);
        final boolean isPublished = post.getPostStatus() == PostStatus.PUBLISHED;
        if (!isPublished || !isFriend)
            throw new ForbiddenPostAccessException(
                    "Forbidden: Cannot access post using comment with id " + comment.getId());

        return post;
    }

    @Override
    public List<CommentAttachment> getCommentAttachmentsBy(Comment comment) {
        if (comment == null)
            return new ArrayList<>();

        return Collections.emptyList();
    }

    @Override
    public List<Comment> getCommentsByPostId(Long postId, PaginationInfo pageInfo, Long lastSeenCommentId) {
        SecurityUtils.throwIfNotAuthenticated();
        if (postId == null)
            throw new InvalidInputException("Post id cannot be null");

        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);

        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        if (isPostOwner(post, authenticatedUserId)) {
            Specification<Comment> spec = getPublishedCommentsAfterIdByAuthorOrSelfForPost(lastSeenCommentId,
                    authenticatedUserId, postId);

            PageRequest pageRequest = createPageRequest(lastSeenCommentId, pageInfo,
                    CommentSpecification.sortDescById());
            return commentRepository.findAll(spec, pageRequest).getContent();
        }

        final boolean isFriend = friendService.isFriend(authenticatedUserId, post.getUser().getId());
        final boolean isBlocked = blockingService.isBlocked(authenticatedUserId, post.getUser().getId());
        if (!isFriend || isBlocked) {
            log.error("Illegal access by user with id {} to retrieve comments for post with id {}", authenticatedUserId,
                    post.getId());
            /*
             * For security purposes, we could also return 403, but 404 is preferable to
             * confuse the malicious actor
             */
            throw new PostNotFoundException();
        }

        Specification<Comment> spec = getPublishedCommentsAfterIdByAuthorOrSelfForPost(lastSeenCommentId,
                authenticatedUserId, postId);

        return commentRepository.findAll(spec,
                createPageRequest(lastSeenCommentId, pageInfo, CommentSpecification.sortDescById())).getContent();
    }

    private Specification<Comment> getPublishedCommentsAfterIdByAuthorOrSelfForPost(Long lastSeenId, Long userId,
            Long postId) {
        return CommentSpecification.afterId(lastSeenId)
                .and(CommentSpecification.hasCommentStatus(CommentStatus.PUBLISHED)
                        .or(CommentSpecification.hasAuthorId(userId)))
                .and(CommentSpecification.hasPostId(postId));
    }

    private PageRequest createPageRequest(Long lastSeenId, PaginationInfo pageInfo, Sort sort) {
        if (lastSeenId == null) {
            return PageRequest.of(pageInfo.getPage() - 1, pageInfo.getPageSize(), sort);
        } else {
            return PageRequest.ofSize(pageInfo.getPageSize()).withSort(sort);
        }
    }

}
