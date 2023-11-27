package com.omarahmed42.socialmedia.service.impl;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.exception.ForbiddenNewsfeedAccessException;
import com.omarahmed42.socialmedia.exception.InternalServerErrorException;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.cache.Newsfeed;
import com.omarahmed42.socialmedia.model.graph.UserNode;
import com.omarahmed42.socialmedia.repository.PostRepository;
import com.omarahmed42.socialmedia.repository.graph.UserNodeRepository;
import com.omarahmed42.socialmedia.service.FanoutService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FanoutServiceImpl implements FanoutService {

    private static final String NEWSFEED_KEY = "newsfeed";
    private static final int MAX_NEWSFEED_SIZE = 250;

    private final CacheManager cacheManager;

    private final PostRepository postRepository;
    private final UserNodeRepository userNodeRepository;

    @Override
    @KafkaListener(topics = "newsfeed")
    public void pushToNewsfeed(ConsumerRecord<String, Newsfeed> consumerRecord) {
        Newsfeed newsfeedPost = consumerRecord.value();
        Cache newsfeedCache = getCache(NEWSFEED_KEY);

        List<UserNode> friends = userNodeRepository.findAllFriendsById(newsfeedPost.getUserId());
        for (UserNode friend : friends) {
            Long friendId = friend.getUserId();
            Set<Newsfeed> retrievedNewsfeed = newsfeedCache.get(friendId, HashSet::new);
            if (retrievedNewsfeed.contains(newsfeedPost))
                return;

            log.info("Retrieved newsfeed: " + retrievedNewsfeed.toString());
            Set<Newsfeed> newsfeed = retrievedNewsfeed.stream()
                    .sorted(Comparator.comparing(Newsfeed::getPostId).reversed())
                    .limit(MAX_NEWSFEED_SIZE - 1L)
                    .collect(Collectors.toSet());

            newsfeed.add(newsfeedPost);
            newsfeedCache.put(friendId, newsfeed);
        }
    }

    private Cache getCache(String cacheName) {
        Cache cache = cacheManager.getCache(NEWSFEED_KEY);
        if (cache == null) {
            log.error("Critical Error: \"" + cacheName + "\" cache does not exist or cannot be created");
            throw new InternalServerErrorException();
        }
        return cache;
    }

    @Override
    @Cacheable(cacheNames = NEWSFEED_KEY, key = "#userId")
    public List<Post> getNewsfeed(Long userId) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        if (!authenticatedUserId.equals(userId)) {
            log.error(
                    "Forbidden: user with id " + authenticatedUserId + " attempted to access an unauthorized resource");
            throw new ForbiddenNewsfeedAccessException("Forbidden access to newsfeed, this attempt will be logged");
        }

        Cache newsfeedCache = getCache(NEWSFEED_KEY);

        Set<Newsfeed> retrievedNewsfeed = newsfeedCache.get(userId, HashSet::new);

        Set<Newsfeed> missingNewsfeed = new HashSet<>();
        List<Post> result = new LinkedList<>();

        Cache postsCache = cacheManager.getCache("posts");
        for (Newsfeed newsfeed : retrievedNewsfeed) {
            Post post = postsCache.get(newsfeed.getPostId(), Post.class);
            if (post == null) {
                missingNewsfeed.add(newsfeed);
            } else {
                result.add(post);
            }
        }

        if (!missingNewsfeed.isEmpty()) {
            List<Long> missingIds = missingNewsfeed.stream()
                    .filter(Objects::nonNull)
                    .map(Newsfeed::getPostId)
                    .distinct()
                    .toList();
            List<Post> remainingPosts = postRepository.findAllById(missingIds);
            cachePosts(remainingPosts);
            result.addAll(remainingPosts);
        }

        return result;
    }

    private void cachePosts(Iterable<Post> posts) {
        Cache postsCache = cacheManager.getCache("posts");
        if (postsCache == null) {
            log.error("""
                    Critical Error: "posts" cache does not exist or cannot be created
                    """);
            return;
        }
        for (Post post : posts) {
            postsCache.put(post.getId(), post);
        }
    }

    @KafkaListener(topics = "newsfeed-following-removal")
    public void removeFromTargetUserNewsfeed(ConsumerRecord<String, Map<String, Long>> consumerRecord) {
        Map<String, Long> values = consumerRecord.value();
        Long targetUserId = values.get("targetUserId");
        Long sourceUserId = values.get("sourceUserId");
        removeFromTargetUserNewsfeed(targetUserId, sourceUserId);
    }

    @Override
    @CachePut(cacheNames = NEWSFEED_KEY, key = "#targetUserId")
    public Set<Newsfeed> removeFromTargetUserNewsfeed(Long targetUserId, Long sourceUserId) {
        if (targetUserId == null || sourceUserId == null)
            throw new IllegalArgumentException("Target/Source user cannot be null");

        if (targetUserId.equals(sourceUserId))
            throw new IllegalArgumentException("target user cannot be the same as the source user");

        Cache newsfeedCache = getCache(NEWSFEED_KEY);

        Set<Newsfeed> newsfeed = newsfeedCache.get(targetUserId, HashSet::new);
        newsfeed.removeIf(nf -> nf.getUserId().equals(sourceUserId));
        return newsfeed;
    }

    @Override
    @CachePut(cacheNames = NEWSFEED_KEY, key = "#targetUserId")
    public Set<Newsfeed> removePostFromNewsfeed(Long targetUserId, Long postId) {
        if (targetUserId == null)
            throw new IllegalArgumentException("Target user cannot be null");

        if (postId == null)
            throw new IllegalArgumentException("Post cannot be null");

        Cache newsfeedCache = getCache(NEWSFEED_KEY);

        Set<Newsfeed> newsfeed = newsfeedCache.get(targetUserId, HashSet::new);
        newsfeed.removeIf(nf -> nf.getPostId().equals(postId));
        return newsfeed;
    }

    @KafkaListener(topics = "newsfeed-post-eviction")
    public void evictNewsfeedByPost(ConsumerRecord<String, Newsfeed> consumerRecord) {
        Newsfeed newsfeedPost = consumerRecord.value();
        Long postOwnerId = newsfeedPost.getUserId();

        List<UserNode> friends = userNodeRepository.findAllFriendsById(postOwnerId);
        Long postId = newsfeedPost.getPostId();
        for (UserNode friend : friends) {
            Long friendId = friend.getUserId();
            removePostFromNewsfeed(friendId, postId);
        }
    }

    @KafkaListener(topics = "newsfeed-user-eviction")
    public void evictNewsfeedByUser(ConsumerRecord<String, Long> consumerRecord) {
        Long userId = consumerRecord.value();
        List<UserNode> friends = userNodeRepository.findAllFriendsById(userId);
        for (UserNode friend : friends) {
            Long friendId = friend.getUserId();
            removeFromTargetUserNewsfeed(friendId, userId);
        }
    }

}
