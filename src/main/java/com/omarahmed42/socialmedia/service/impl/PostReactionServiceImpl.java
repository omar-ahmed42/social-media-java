package com.omarahmed42.socialmedia.service.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.enums.PostStatus;
import com.omarahmed42.socialmedia.exception.ForbiddenPostAccessException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.PostNotFoundException;
import com.omarahmed42.socialmedia.exception.ReactionNotFoundException;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.PostReaction;
import com.omarahmed42.socialmedia.model.PostReactionId;
import com.omarahmed42.socialmedia.model.Reaction;
import com.omarahmed42.socialmedia.repository.PostReactionRepository;
import com.omarahmed42.socialmedia.repository.PostRepository;
import com.omarahmed42.socialmedia.repository.ReactionRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.service.BlockingService;
import com.omarahmed42.socialmedia.service.FriendService;
import com.omarahmed42.socialmedia.service.PostReactionService;
import com.omarahmed42.socialmedia.service.StatisticsService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PostReactionServiceImpl implements PostReactionService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    private final ReactionRepository reactionRepository;
    private final PostReactionRepository postReactionRepository;

    private final BlockingService blockingService;
    private final FriendService friendService;

    private final StatisticsService statisticsService;

    public PostReactionServiceImpl(UserRepository userRepository, PostRepository postRepository,
            ReactionRepository reactionRepository, PostReactionRepository postReactionRepository,
            BlockingService blockingService, FriendService friendService,
            @Qualifier("postReactionsStatisticsService") StatisticsService statisticsService) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.reactionRepository = reactionRepository;
        this.postReactionRepository = postReactionRepository;
        this.blockingService = blockingService;
        this.friendService = friendService;
        this.statisticsService = statisticsService;
    }

    @Override
    public PostReaction savePostReaction(Integer reactionId, Long postId) {
        SecurityUtils.throwIfNotAuthenticated();

        log.info("Finding post with id {}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        final boolean isPublished = (post.getPostStatus() == PostStatus.PUBLISHED);
        if (isPublished && isPostOwner(post, authenticatedUserId)) {
            PostReactionId postReactionId = new PostReactionId();
            postReactionId.setUser(userRepository.getReferenceById(authenticatedUserId));
            postReactionId.setPost(post);
            return createOrUpdatePostReaction(postReactionId, postId, reactionId);
        } else if (!isPublished && isPostOwner(post, authenticatedUserId)) {
            throw new InvalidInputException("Cannot react to a non-published post");
        }

        Long postOwnerId = post.getUser().getId();
        if (!isPublished
                || blockingService.isBlocked(postOwnerId, authenticatedUserId)
                || !friendService.isFriend(authenticatedUserId, postOwnerId)) {
            throw new ForbiddenPostAccessException("Forbidden: cannot access post with id " + postId);
        }

        PostReactionId postReactionId = new PostReactionId();
        postReactionId.setUser(userRepository.getReferenceById(authenticatedUserId));
        postReactionId.setPost(post);

        return createOrUpdatePostReaction(postReactionId, postId, reactionId);
    }

    private PostReaction createOrUpdatePostReaction(PostReactionId postReactionId, Long postId, Integer reactionId) {
        PostReaction retrievedPostReaction = postReactionRepository.findById(postReactionId).orElse(null);
        final Integer oldReactionId = retrievedPostReaction == null ? null
                : retrievedPostReaction.getReaction().getId();

        if (retrievedPostReaction != null && (oldReactionId == null && reactionId == null))
            return retrievedPostReaction;

        PostReaction postReaction = new PostReaction(postReactionId);
        postReaction.setReaction(reactionId == null ? null : reactionRepository.getReferenceById(reactionId));
        postReaction = postReactionRepository.save(postReaction);
        if (oldReactionId != null) {
            Reaction reaction = reactionRepository.findById(oldReactionId).orElseThrow(
                    () -> new ReactionNotFoundException("Reaction with id " + oldReactionId + " not found"));
            statisticsService.decrement(postId.toString(), reaction.getName());
        }

        if (reactionId != null) {
            Reaction reaction = reactionRepository.findById(reactionId).orElseThrow(
                    () -> new ReactionNotFoundException("Reaction with id " + reactionId + " not found"));
            statisticsService.increment(postId.toString(), reaction.getName());
        }

        return postReaction;
    }

    private boolean isPostOwner(Post post, Long userId) {
        Long postOwnerId = post.getUser().getId();
        return postOwnerId.equals(userId);
    }

    @Override
    public PostReaction removePostReaction(Long postId) {
        return savePostReaction(null, postId);
    }

    
}