package com.omarahmed42.socialmedia.service.impl;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.dto.response.ReactionStatistics;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.model.Reaction;
import com.omarahmed42.socialmedia.repository.CommentReactionRepository;
import com.omarahmed42.socialmedia.repository.ReactionRepository;
import com.omarahmed42.socialmedia.service.AsyncService;
import com.omarahmed42.socialmedia.service.StatisticsService;

@Service("commentReactionsStatisticsService")
public class CommentReactionsStatisticsService implements StatisticsService {

    private final AsyncService asyncService;

    private final CommentReactionRepository commentReactionRepository;
    private final ReactionRepository reactionRepository;
    private final RedisTemplate<String, Long> redisTemplate;

    private static final String COMMENT_PREFIX = "comment";
    private static final String KEY_DELIMITER = ":";

    private Set<String> validActivityTypes;

    public CommentReactionsStatisticsService(CommentReactionRepository commentReactionRepository,
            ReactionRepository reactionRepository, RedisTemplate<String, Long> redisTemplate,
            AsyncService asyncService) {
        this.commentReactionRepository = commentReactionRepository;
        this.reactionRepository = reactionRepository;
        this.redisTemplate = redisTemplate;
        this.asyncService = asyncService;

        this.validActivityTypes = new HashSet<>();

        List<Reaction> reactions = this.reactionRepository.findAll();
        this.validActivityTypes = reactions
                .stream()
                .filter(Objects::nonNull)
                .map(Reaction::getName)
                .filter(StringUtils::isNotBlank).distinct()
                .collect(Collectors.toSet());
    }

    @Override
    public Long get(String commentId, String activityType) {
        if (commentId == null)
            throw new IllegalArgumentException("Comment id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        final String key = key(commentId, activityType);
        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        Long count = ops.get(key);
        if (count == null)
            count = commentReactionRepository.countByReactionNameAndCommentReactionId_Comment_id(activityType,
                    Long.parseLong(commentId));

        ops.set(key, count, Duration.ofHours(12));
        return count;
    }

    @Override
    public void increment(String commentId, String activityType, Long value) {
        if (commentId == null)
            throw new IllegalArgumentException("Comment id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        if (value == null)
            throw new IllegalArgumentException("Value cannot be null");

        if (value < 1)
            throw new IllegalArgumentException("Value cannot be less than 1");

        if (!isValid(activityType))
            throw new IllegalArgumentException("Invalid activity type");

        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        final String key = key(commentId, activityType);

        ops.setIfAbsent(key, commentReactionRepository.countByReactionNameAndCommentReactionId_Comment_id(activityType,
                Long.parseLong(commentId)), Duration.ofHours(12));
        ops.increment(key, value);
    }

    @Override
    public void increment(String commentId, String activityType) {
        if (commentId == null)
            throw new IllegalArgumentException("Comment id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        if (!isValid(activityType))
            throw new IllegalArgumentException("Invalid activity type");

        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        final String key = key(commentId, activityType);

        ops.setIfAbsent(key, commentReactionRepository.countByReactionNameAndCommentReactionId_Comment_id(activityType,
                Long.parseLong(commentId)), Duration.ofHours(12)); // fix this
        ops.increment(key);
    }

    private String key(String commentId, String activityType) {
        return COMMENT_PREFIX + KEY_DELIMITER + commentId + KEY_DELIMITER + activityType;
    }

    private boolean isValid(String activityType) {
        return validActivityTypes.contains(activityType);
    }

    @Override
    public void decrement(String commentId, String activityType, Long value) {
        if (commentId == null)
            throw new IllegalArgumentException("Comment id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        if (value == null)
            throw new IllegalArgumentException("Value cannot be null");

        if (value < 1)
            throw new IllegalArgumentException("Value cannot be less than 1");

        if (!isValid(activityType))
            throw new IllegalArgumentException("Invalid activity type");

        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        final String key = key(commentId, activityType);

        ops.setIfAbsent(key, commentReactionRepository.countByReactionNameAndCommentReactionId_Comment_id(activityType,
                Long.parseLong(commentId)), Duration.ofHours(12));
        ops.decrement(key, value);
    }

    @Override
    public void decrement(String commentId, String activityType) {
        if (commentId == null)
            throw new IllegalArgumentException("Comment id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        if (!isValid(activityType))
            throw new IllegalArgumentException("Invalid activity type");

        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        final String key = key(commentId, activityType);

        ops.setIfAbsent(key, commentReactionRepository.countByReactionNameAndCommentReactionId_Comment_id(activityType,
                Long.parseLong(commentId)), Duration.ofHours(12));
        ops.decrement(key);
    }

    @Override
    public Object getStatistics(String commentId) {
        if (commentId == null)
            throw new InvalidInputException("Comment id cannot be null");

        ReactionStatistics reactionStatistics = new ReactionStatistics();
        CompletableFuture<Long> likeCountFuture = asyncService.getCompletable(() -> getCountAsync(commentId, "like"));
        CompletableFuture<Long> loveCountFuture = asyncService.getCompletable(() -> getCountAsync(commentId, "love"));
        CompletableFuture<Long> angryCountFuture = asyncService.getCompletable(() -> getCountAsync(commentId, "angry"));
        CompletableFuture<Long> sadCountFuture = asyncService.getCompletable(() -> getCountAsync(commentId, "sad"));
        CompletableFuture<Long> laughCountFuture = asyncService.getCompletable(() -> getCountAsync(commentId, "laugh"));

        CompletableFuture.allOf(likeCountFuture, loveCountFuture, angryCountFuture, sadCountFuture, laughCountFuture)
                .join();
        reactionStatistics.setLikeCount(likeCountFuture.join());
        reactionStatistics.setLoveCount(loveCountFuture.join());
        reactionStatistics.setAngryCount(angryCountFuture.join());
        reactionStatistics.setSadCount(sadCountFuture.join());
        reactionStatistics.setLaughCount(laughCountFuture.join());

        return reactionStatistics;
    }

    private Future<Long> getCountAsync(String commentId, String activityType) {
        if (commentId == null)
            throw new IllegalArgumentException("Post id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        final String key = key(commentId, activityType);
        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        Long count = ops.get(key);
        if (count == null)
            count = commentReactionRepository.countByReactionNameAndCommentReactionId_Comment_id(activityType,
                    Long.parseLong(commentId));

        ops.set(key, count, Duration.ofHours(12));
        return CompletableFuture.completedFuture(count);
    }

}
