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
import com.omarahmed42.socialmedia.repository.PostReactionRepository;
import com.omarahmed42.socialmedia.repository.ReactionRepository;
import com.omarahmed42.socialmedia.service.AsyncService;
import com.omarahmed42.socialmedia.service.StatisticsService;

import lombok.extern.slf4j.Slf4j;

@Service("postReactionsStatisticsService")
@Slf4j
public class PostReactionsStatisticsService implements StatisticsService {

    private final PostReactionRepository postReactionRepository;
    private final ReactionRepository reactionRepository;
    private final RedisTemplate<String, Long> redisTemplate;
    private final AsyncService asyncService;

    private static final String POST_PREFIX = "post";
    private static final String KEY_DELIMITER = ":";

    private Set<String> validActivityTypes;

    public PostReactionsStatisticsService(PostReactionRepository postReactionRepository,
            ReactionRepository reactionRepository, RedisTemplate<String, Long> redisTemplate,
            AsyncService asyncService) {
        this.postReactionRepository = postReactionRepository;
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
    public Long get(String postId, String activityType) {
        if (postId == null)
            throw new IllegalArgumentException("Post id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        final String key = key(postId, activityType);
        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        Long count = ops.get(key);
        if (count == null)
            count = postReactionRepository.countByReactionNameAndPostReactionId_Post_id(activityType,
                    Long.parseLong(postId));

        ops.set(key, count, Duration.ofHours(12));
        return count;
    }

    @Override
    public void increment(String postId, String activityType, Long value) {
        if (postId == null)
            throw new IllegalArgumentException("Post id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        if (value == null)
            throw new IllegalArgumentException("Value cannot be null");

        if (value < 1)
            throw new IllegalArgumentException("Value cannot be less than 1");

        if (!isValid(activityType))
            throw new IllegalArgumentException("Invalid activity type");

        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        final String key = key(postId, activityType);

        ops.setIfAbsent(key, postReactionRepository.countByReactionNameAndPostReactionId_Post_id(activityType,
                Long.parseLong(postId)), Duration.ofHours(12));
        ops.increment(key, value);
    }

    @Override
    public void increment(String postId, String activityType) {
        if (postId == null)
            throw new IllegalArgumentException("Post id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        if (!isValid(activityType))
            throw new IllegalArgumentException("Invalid activity type");

        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        final String key = key(postId, activityType);

        Long count = postReactionRepository.countByReactionNameAndPostReactionId_Post_id(activityType,
                Long.parseLong(postId));
        log.info("COUNT: {}", count);
        ops.setIfAbsent(key, count - 1, Duration.ofHours(12));
        ops.increment(key);
    }

    private String key(String postId, String activityType) {
        return POST_PREFIX + KEY_DELIMITER + postId + KEY_DELIMITER + activityType;
    }

    private boolean isValid(String activityType) {
        return validActivityTypes.contains(activityType);
    }

    @Override
    public void decrement(String postId, String activityType, Long value) {
        if (postId == null)
            throw new IllegalArgumentException("Post id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        if (value == null)
            throw new IllegalArgumentException("Value cannot be null");

        if (value < 1)
            throw new IllegalArgumentException("Value cannot be less than 1");

        if (!isValid(activityType))
            throw new IllegalArgumentException("Invalid activity type");

        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        final String key = key(postId, activityType);

        ops.setIfAbsent(key, postReactionRepository.countByReactionNameAndPostReactionId_Post_id(activityType,
                Long.parseLong(postId)), Duration.ofHours(12));
        ops.decrement(key, value);
    }

    @Override
    public void decrement(String postId, String activityType) {
        if (postId == null)
            throw new IllegalArgumentException("Post id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        if (!isValid(activityType))
            throw new IllegalArgumentException("Invalid activity type");

        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        final String key = key(postId, activityType);

        ops.setIfAbsent(key, postReactionRepository.countByReactionNameAndPostReactionId_Post_id(activityType,
                Long.parseLong(postId)), Duration.ofHours(12));
        ops.decrement(key);
    }

    @Override
    public Object getStatistics(String postId) {
        if (postId == null)
            throw new InvalidInputException("Post id cannot be null");

        CompletableFuture<Long> likeCountFuture = asyncService.getCompletable(() -> getCountAsync(postId, "like"));
        CompletableFuture<Long> loveCountFuture = asyncService.getCompletable(() -> getCountAsync(postId, "love"));
        CompletableFuture<Long> angryCountFuture = asyncService.getCompletable(() -> getCountAsync(postId, "angry"));
        CompletableFuture<Long> sadCountFuture = asyncService.getCompletable(() -> getCountAsync(postId, "sad"));
        CompletableFuture<Long> laughCountFuture = asyncService.getCompletable(() -> getCountAsync(postId, "laugh"));

        CompletableFuture.allOf(likeCountFuture, loveCountFuture, angryCountFuture, sadCountFuture, laughCountFuture)
                .join();

        ReactionStatistics reactionStatistics = new ReactionStatistics();
        reactionStatistics.setLikeCount(likeCountFuture.join());
        reactionStatistics.setLoveCount(loveCountFuture.join());
        reactionStatistics.setAngryCount(angryCountFuture.join());
        reactionStatistics.setSadCount(sadCountFuture.join());
        reactionStatistics.setLaughCount(laughCountFuture.join());

        return reactionStatistics;
    }

    private Future<Long> getCountAsync(String postId, String activityType) {
        if (postId == null)
            throw new IllegalArgumentException("Post id cannot be null");

        if (StringUtils.isBlank(activityType))
            throw new IllegalArgumentException("Activity type cannot be empty");

        final String key = key(postId, activityType);
        ValueOperations<String, Long> ops = redisTemplate.opsForValue();
        Long count = ops.get(key);
        if (count == null)
            count = postReactionRepository.countByReactionNameAndPostReactionId_Post_id(activityType,
                    Long.parseLong(postId));

        ops.set(key, count, Duration.ofHours(12));
        return CompletableFuture.completedFuture(count);
    }

}
