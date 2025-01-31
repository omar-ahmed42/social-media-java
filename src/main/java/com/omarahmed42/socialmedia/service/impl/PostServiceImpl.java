package com.omarahmed42.socialmedia.service.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.dto.event.AttachmentDeletionEvent;
import com.omarahmed42.socialmedia.enums.PostStatus;
import com.omarahmed42.socialmedia.exception.ForbiddenPostAccessException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.PostNotFoundException;
import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.PostAttachment;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.model.cache.Newsfeed;
import com.omarahmed42.socialmedia.projection.PostInputProjection;
import com.omarahmed42.socialmedia.repository.PostAttachmentRepository;
import com.omarahmed42.socialmedia.repository.PostRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.service.FileService;
import com.omarahmed42.socialmedia.service.FriendService;
import com.omarahmed42.socialmedia.service.PostService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostAttachmentRepository postAttachmentsRepository;
    private final UserRepository userRepository;
    private final FriendService friendService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CacheManager cacheManager;
    private final FileService fileService;

    @Override
    public Post addPost(PostInputProjection postInputProjection) {
        Post post = null;
        PostStatus status = PostStatus.valueOf(postInputProjection.getPostStatus());
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        switch (status) {
            case ARCHIVED:
                post = archivePost(authenticatedUserId,
                        postInputProjection);
                break;
            case DRAFT:
                post = savePostAsDraft(authenticatedUserId, postInputProjection);
                break;
            case PUBLISHED:
                post = publishPost(authenticatedUserId, postInputProjection);
                break;
            default:
                throw new InvalidInputException("Invalid post status");
        }

        return post;
    }

    private Post archivePost(Long userId, PostInputProjection postInputProjection) {
        if (postInputProjection.getId() == null)
            throw new InvalidInputException("Cannot archive a non-existing post");

        Post post = postRepository.findById(postInputProjection.getId())
                .orElseThrow(PostNotFoundException::new);

        throwIfNotPostOwner(post, userId);

        if (post.getPostStatus() == PostStatus.DRAFT) {
            throw new InvalidInputException("Cannot archive a draft post");
        }

        if (post.getPostStatus() == PostStatus.ARCHIVED) {
            return post;
        }

        post.setPostStatus(PostStatus.ARCHIVED);
        post = postRepository.save(post);
        return post;
    }

    private boolean isPostOwner(Post post, Long userId) {
        Long postOwnerId = post.getUser().getId();
        return postOwnerId.equals(userId);
    }

    private Post savePostAsDraft(Long userId, PostInputProjection postInputProjection) {
        if (postInputProjection.getId() == null) {
            Post post = new Post(postInputProjection.getContent(),
                    PostStatus.DRAFT,
                    userRepository.getReferenceById(userId));
            validateAndSetParentPost(userId, postInputProjection, post);
            post = postRepository.save(post);
            return post;
        }

        Post post = postRepository.findById(postInputProjection.getId())
                .orElseThrow(PostNotFoundException::new);
        validateAndSetParentPost(userId, postInputProjection, post);

        throwIfNotPostOwner(post, userId);
        throwIfNotDraftPost(post.getPostStatus());

        if (Objects.equals(post.getContent(), postInputProjection.getContent())) {
            return post;
        }

        post.setContent(postInputProjection.getContent());
        post = postRepository.save(post);
        return post;
    }

    private void validateAndSetParentPost(Long userId, PostInputProjection postInputProjection, Post post) {
        Optional.ofNullable(postInputProjection.getParentId()).ifPresent(parentId -> {
            Post sharedPost = postRepository.findById(postInputProjection.getParentId())
                    .orElseThrow(PostNotFoundException::new);
            validateParentPost(userId, sharedPost);
            post.setParent(sharedPost);
        });
    }

    private void validateParentPost(Long userId, Post sharedPost) {
        if (sharedPost.getPostStatus() != PostStatus.PUBLISHED || !canView(sharedPost, userId)) {
            log.warn("Illegal access to post {} with status {} by user id {}", sharedPost.getId(),
                    sharedPost.getPostStatus(), userId);
            // Throw not found for security purposes
            throw new PostNotFoundException("Post not found or inaccessible");
        }
    }

    private boolean canView(Post post, Long userId) {
        Long ownerId = post.getUser().getId();
        // Only friends and post owners can view posts
        return isPostOwner(post, userId) || friendService.isFriend(userId, ownerId);
    }

    private void throwIfNotPostOwner(Post post, Long userId) {
        if (!isPostOwner(post, userId))
            throw new ForbiddenPostAccessException("Forbidden: Cannot access post with id " + post.getId());
    }

    private void throwIfNotDraftPost(PostStatus postStatus) {
        if (postStatus != PostStatus.DRAFT)
            throw new InvalidInputException("Cannot draft a non-draft post");
    }

    private void throwIfBlankContent(String content) {
        if (StringUtils.isBlank(content))
            throw new InvalidInputException("Post must have non-empty content");
    }

    private Post publishPost(Long userId, PostInputProjection postInputProjection) {
        if (postInputProjection.getId() == null) {
            throwIfBlankContent(postInputProjection.getContent());
            Post post = new Post(postInputProjection.getContent(),
                    PostStatus.PUBLISHED,
                    userRepository.getReferenceById(userId));
            validateAndSetParentPost(userId, postInputProjection, post);
            post = postRepository.save(post);
            kafkaTemplate.send("newsfeed", new Newsfeed(userId, post.getId()));
            return post;
        }

        Post post = postRepository.findById(postInputProjection.getId())
                .orElseThrow(PostNotFoundException::new);
        throwIfNotPostOwner(post, userId);
        throwIfBlankContentAndNoAttachments(postInputProjection.getContent(), post);

        final PostStatus oldPostStatus = PostStatus.valueOf(post.getPostStatus().toString());

        if (isEqualContent(post, postInputProjection) && oldPostStatus == PostStatus.PUBLISHED)
            return post;

        boolean wasDraft = oldPostStatus == PostStatus.DRAFT;
        if (wasDraft)
            validateAndSetParentPost(userId, postInputProjection, post);

        post.setContent(postInputProjection.getContent());
        post.setPostStatus(PostStatus.PUBLISHED);
        post = postRepository.save(post);
        if (wasDraft)
            kafkaTemplate.send("newsfeed", new Newsfeed(userId, post.getId()));

        cache(post);

        return post;
    }

    private void cache(Post post) {
        Cache postsCache = cacheManager.getCache("posts");
        if (postsCache == null) {
            log.error("Critical Error: \"posts\" cache does not exist or cannot be created");
            return;
        }
        postsCache.put(post.getId(), post);
    }

    private void throwIfBlankContentAndNoAttachments(String content, Post post) {
        List<PostAttachment> postAttachments = postAttachmentsRepository.findAllByPostAttachmentIdPost(post);
        // List<PostAttachment> postAttachments = post.getPostAttachments();
        boolean isEmptyPost = StringUtils.isBlank(content) && (postAttachments == null || postAttachments.isEmpty());
        if (isEmptyPost)
            throw new InvalidInputException("Post must at least have non-blank content or uploaded attachments");
    }

    private boolean isEqualContent(Post postWithOldContent, PostInputProjection inputWithNewContent) {
        if (postWithOldContent == null || inputWithNewContent == null)
            return false;
        return StringUtils.equals(postWithOldContent.getContent(), inputWithNewContent.getContent());
    }

    @Override
    @Transactional
    public Long deletePost(Long postId) {
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        if (!isPostOwner(post, authenticatedUserId)) {
            log.warn("Illegal access to post {} by user id {}", post.getId(), authenticatedUserId);
            // Throw not found for security purposes
            throw new PostNotFoundException("Post not found or inaccessible");
        }
        // Check if the post has been shared by other posts
        boolean isShared = postRepository.existsByParentId(postId);
        List<PostAttachment> postAttachments = postAttachmentsRepository.findAllByPostAttachmentIdPost(post);

        // Extract unique attachment URLs
        List<String> attachmentUrls = postAttachments.stream()
                .map(a -> a.getPostAttachmentId().getAttachment().getUrl())
                .distinct()
                .toList();

        if (attachmentUrls != null && !attachmentUrls.isEmpty()) {
            // Send attachment deletion event to Kafka
            kafkaTemplate.send("attachment-deletion", new AttachmentDeletionEvent(attachmentUrls));
        }

        if (isShared) {
            // Tombstone the post
            log.info("Tombstoning post {} by user {}", postId, authenticatedUserId);
            post.setPostStatus(PostStatus.TOMBSTONE);
            post.setContent(null); // Set content to null to save space
            post.getPostAttachments().clear();
            postRepository.save(post);
        } else {
            // Hard delete the post
            log.info("Hard deleting post {} by user {}", postId, authenticatedUserId);
            postRepository.delete(post);
        }

        evictCache(postId);
        kafkaTemplate.send("newsfeed-post-eviction", new Newsfeed(authenticatedUserId, postId));
        return postId;
    }

    private void evictCache(Long postId) {
        Cache postsCache = cacheManager.getCache("posts");
        if (postsCache == null) {
            log.error("Critical Error: \"posts\" cache does not exist or cannot be created");
            return;
        }
        postsCache.evict(postId);
    }

    @KafkaListener(topics = "attachment-deletion")
    public void handleAttachmentDeletion(AttachmentDeletionEvent event) {
        List<String> attachmentUrls = event.getAttachmentUrls();
        attachmentUrls.forEach(url -> {
            try {
                fileService.remove(Path.of(url));
            } catch (IOException e) {
                log.error("Failed to delete file: {}", url, e);
            }
        });
    }

    @Override
    public Post findPost(Long postId) {
        if (postId == null)
            throw new InvalidInputException("Id cannot be null");

        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

        Post post = postRepository
                .findById(postId)
                .orElseThrow(PostNotFoundException::new);

        if (isPostOwner(post, authenticatedUserId))
            return post;

        Long postOwnerId = post.getUser().getId();
        final boolean isFriend = friendService.isFriend(authenticatedUserId, postOwnerId);
        if ((post.getPostStatus() == PostStatus.PUBLISHED || post.getPostStatus() == PostStatus.TOMBSTONE) && isFriend) {
            return post;
        }

        throw new ForbiddenPostAccessException("Forbidden: Cannot access post with id " + postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> getCommentsBy(Post post) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

        if (isPostOwner(post, authenticatedUserId))
            return postRepository.findAllCommentsById(post.getId());

        Long postOwnerId = post.getUser().getId();
        final boolean isFriend = friendService.isFriend(authenticatedUserId, postOwnerId);
        if (post.getPostStatus() == PostStatus.PUBLISHED && isFriend)
            return postRepository.findAllCommentsById(postOwnerId);

        throw new ForbiddenPostAccessException("Forbidden: Cannot access post with id " + post.getId());
    }

    @Override
    public List<Post> getPostsBy(User user) {
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        Long userId = user.getId();
        if (userId.equals(authenticatedUserId))
            return postRepository.findAllByUser(user);

        final boolean isFriend = friendService.isFriend(authenticatedUserId, userId);
        if (!isFriend)
            throw new ForbiddenPostAccessException("Forbidden: Cannot access posts for user with id " + userId);

        List<Post> posts = postRepository.findAllByUser(user);
        if (posts == null || posts.isEmpty())
            return new ArrayList<>();

        return posts.stream()
                .filter(m -> m.getPostStatus() == PostStatus.PUBLISHED)
                .toList();
    }

    @Override
    public List<Post> findPostsByUserId(Long userId, PaginationInfo pageInfo, Long lastSeenPostId) {
        if (userId == null)
            throw new IllegalArgumentException("User id cannot be empty");
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        if (userId.equals(authenticatedUserId)) {
            if (lastSeenPostId != null)
                return postRepository.findAllByUserIdAndLastSeenPostId(userId, lastSeenPostId, pageInfo.getPageSize());
            return postRepository.findAllByUserId(userId,
                    PageRequest.of(pageInfo.getPage() - 1, pageInfo.getPageSize(), Sort.by(Direction.DESC, "id")));
        }

        final boolean isFriend = friendService.isFriend(authenticatedUserId, userId);
        if (!isFriend)
            throw new ForbiddenPostAccessException("Forbidden: Cannot access posts for user with id " + userId);

        if (lastSeenPostId != null)
            return postRepository.findAllByUserIdAndLastSeenPostIdAndPostStatus(userId, lastSeenPostId,
                    PostStatus.PUBLISHED, pageInfo.getPageSize());
        return postRepository.findAllByUserIdAndPostStatus(userId, PostStatus.PUBLISHED,
                PageRequest.of(pageInfo.getPage() - 1, pageInfo.getPageSize(), Sort.by(Sort.Direction.DESC, "id")));
    }

}