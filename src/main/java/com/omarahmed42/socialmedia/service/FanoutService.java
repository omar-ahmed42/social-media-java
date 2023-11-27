package com.omarahmed42.socialmedia.service;

import java.util.List;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.cache.Newsfeed;

public interface FanoutService {
    void pushToNewsfeed(ConsumerRecord<String, Newsfeed> consumerRecord);

    List<Post> getNewsfeed(Long userId);

    Set<Newsfeed> removeFromTargetUserNewsfeed(Long targetUserId, Long sourceUserId);

    Set<Newsfeed> removePostFromNewsfeed(Long targetUserId, Long postId);

    void evictNewsfeedByUser(ConsumerRecord<String, Long> consumerRecord);

    void evictNewsfeedByPost(ConsumerRecord<String, Newsfeed> consumerRecord);
}
