package com.omarahmed42.socialmedia.service.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.enums.CommentStatus;
import com.omarahmed42.socialmedia.enums.PostStatus;
import com.omarahmed42.socialmedia.exception.CommentNotFoundException;
import com.omarahmed42.socialmedia.exception.ForbiddenPostAccessException;
import com.omarahmed42.socialmedia.exception.ReactionNotFoundException;
import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.CommentReaction;
import com.omarahmed42.socialmedia.model.CommentReactionId;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.Reaction;
import com.omarahmed42.socialmedia.repository.CommentReactionRepository;
import com.omarahmed42.socialmedia.repository.CommentRepository;
import com.omarahmed42.socialmedia.repository.ReactionRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.service.BlockingService;
import com.omarahmed42.socialmedia.service.CommentReactionService;
import com.omarahmed42.socialmedia.service.FriendService;
import com.omarahmed42.socialmedia.service.StatisticsService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

@Service
public class CommentReactionServiceImpl implements CommentReactionService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    private final ReactionRepository reactionRepository;
    private final CommentReactionRepository commentReactionRepository;

    private final BlockingService blockingService;
    private final FriendService friendService;

    private final StatisticsService statisticsService;

    public CommentReactionServiceImpl(UserRepository userRepository, CommentRepository commentRepository,
            ReactionRepository reactionRepository, CommentReactionRepository commentReactionRepository,
            BlockingService blockingService, FriendService friendService,
            @Qualifier("commentReactionsStatisticsService") StatisticsService statisticsService) {
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.commentReactionRepository = commentReactionRepository;
        this.blockingService = blockingService;
        this.friendService = friendService;
        this.statisticsService = statisticsService;
    }

    @Override
    public CommentReaction saveCommentReaction(Integer reactionId, Long commentId) {
        SecurityUtils.throwIfNotAuthenticated();

        Comment comment = commentRepository.findCommentById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        Post post = comment.getPost();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        final boolean isPostPublished = (post.getPostStatus() == PostStatus.PUBLISHED);
        final boolean isCommentPublished = (comment.getCommentStatus() == CommentStatus.PUBLISHED);
        final boolean isPublished = isPostPublished && isCommentPublished;
        if (isPublished && isPostOwner(post, authenticatedUserId)) {
            CommentReactionId commentReactionId = new CommentReactionId();
            commentReactionId.setUser(userRepository.getReferenceById(authenticatedUserId));
            commentReactionId.setComment(comment);

            CommentReaction commentReaction = new CommentReaction(commentReactionId);
            commentReaction.setReaction(reactionRepository.getReferenceById(reactionId));
            commentReaction = commentReactionRepository.save(commentReaction);
            return commentReaction;
        }

        Long postOwnerId = post.getUser().getId();
        final boolean canViewPost = (!blockingService.isBlocked(postOwnerId, authenticatedUserId)
                || friendService.isFriend(authenticatedUserId, postOwnerId));

        Long commentOwnerId = comment.getUser().getId();
        final boolean canViewComment = (!blockingService.isBlocked(commentOwnerId, authenticatedUserId)
                || friendService.isFriend(authenticatedUserId, commentOwnerId));

        if (!isPublished || !canViewPost || !canViewComment)
            throw new ForbiddenPostAccessException("Forbidden: cannot access comment with id " + commentId);

        CommentReactionId commentReactionId = new CommentReactionId();
        commentReactionId.setUser(userRepository.getReferenceById(authenticatedUserId));
        commentReactionId.setComment(comment);

        CommentReaction retrievedCommentReaction = commentReactionRepository.findById(commentReactionId).orElse(null);
        final Integer oldReactionId = retrievedCommentReaction == null ? null
                : retrievedCommentReaction.getReaction().getId();

        if (retrievedCommentReaction != null && (oldReactionId == null && reactionId == null))
            return retrievedCommentReaction;

        CommentReaction commentReaction = new CommentReaction(commentReactionId);
        commentReaction.setReaction(reactionRepository.getReferenceById(reactionId));
        commentReaction = commentReactionRepository.save(commentReaction);
        if (oldReactionId != null) {
            Reaction reaction = reactionRepository.findById(oldReactionId).orElseThrow(
                    () -> new ReactionNotFoundException("Reaction with id " + oldReactionId + " not found"));
            statisticsService.decrement(commentId.toString(), reaction.getName());
        }

        if (reactionId != null) {
            Reaction reaction = reactionRepository.findById(reactionId).orElseThrow(
                    () -> new ReactionNotFoundException("Reaction with id " + reactionId + " not found"));
            statisticsService.increment(commentId.toString(), reaction.getName());
        }

        return commentReaction;
    }

    private boolean isPostOwner(Post post, Long userId) {
        Long postOwnerId = post.getUser().getId();
        return postOwnerId.equals(userId);
    }
}
