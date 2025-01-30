package com.omarahmed42.socialmedia.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
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
            post = postRepository.save(post);
            return post;
        }

        Post post = postRepository.findById(postInputProjection.getId())
                .orElseThrow(PostNotFoundException::new);

        throwIfNotPostOwner(post, userId);
        throwIfNotDraftPost(post.getPostStatus());

        if (Objects.equals(post.getContent(), postInputProjection.getContent())) {
            return post;
        }

        post.setContent(postInputProjection.getContent());
        post = postRepository.save(post);
        return post;
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

        post.setContent(postInputProjection.getContent());
        post.setPostStatus(PostStatus.PUBLISHED);
        post = postRepository.save(post);
        if (oldPostStatus == PostStatus.DRAFT)
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
    public Integer deletePost(Long postId) {
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        int recordCount = postRepository.deleteByIdAndUserId(postId, authenticatedUserId);
        if (recordCount == 1) {
            evictCache(postId);
            kafkaTemplate.send("newsfeed-post-eviction", new Newsfeed(authenticatedUserId, postId));
        }
        return recordCount;
    }

    private void evictCache(Long postId) {
        Cache postsCache = cacheManager.getCache("posts");
        if (postsCache == null) {
            log.error("Critical Error: \"posts\" cache does not exist or cannot be created");
            return;
        }
        postsCache.evict(postId);
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
        if (post.getPostStatus() == PostStatus.PUBLISHED && isFriend) {
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
            if (userId == null) throw new IllegalArgumentException("User id cannot be empty");
            Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
            if (userId.equals(authenticatedUserId)) {
                if (lastSeenPostId != null) return postRepository.findAllByUserIdAndLastSeenPostId(userId, lastSeenPostId, pageInfo.getPageSize());
                return postRepository.findAllByUserId(userId, PageRequest.of(pageInfo.getPage() - 1, pageInfo.getPageSize(), Sort.by(Direction.DESC, "id")));
            }

            final boolean isFriend = friendService.isFriend(authenticatedUserId, userId);
            if (!isFriend)
                throw new ForbiddenPostAccessException("Forbidden: Cannot access posts for user with id " + userId);
            
            
            if (lastSeenPostId != null) return postRepository.findAllByUserIdAndLastSeenPostIdAndPostStatus(userId, lastSeenPostId, PostStatus.PUBLISHED, pageInfo.getPageSize());
            return postRepository.findAllByUserIdAndPostStatus(userId, PostStatus.PUBLISHED, PageRequest.of(pageInfo.getPage() - 1, pageInfo.getPageSize(), Sort.by(Sort.Direction.DESC, "id")));
    }

}