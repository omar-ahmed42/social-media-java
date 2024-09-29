package com.omarahmed42.socialmedia.service;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.dto.event.FriendRequestEvent;
import com.omarahmed42.socialmedia.model.User;

public interface FriendService {

    boolean isFriend(Long userId, Long postOwnerId);

    boolean isFriend(Long friendId);

    boolean unfriend(Long friendId);

    void consume(ConsumerRecord<String, FriendRequestEvent> kafkaRecord);

    List<User> findFriends(PaginationInfo paginationInfo);

    Long countFriends(Long userId);

}
