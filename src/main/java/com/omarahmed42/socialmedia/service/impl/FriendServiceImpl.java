package com.omarahmed42.socialmedia.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.dto.event.FriendRequestEvent;
import com.omarahmed42.socialmedia.enums.FriendRequestStatus;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.UserNotFoundException;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.model.graph.UserNode;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.repository.graph.UserNodeRepository;
import com.omarahmed42.socialmedia.service.FriendService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserNodeRepository userNodeRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        return userNodeRepository.isFriend(userId, friendId);
    }

    @KafkaListener(topics = "friend-request")
    public void consume(ConsumerRecord<String, FriendRequestEvent> kafkaRecord) {
        FriendRequestEvent friendRequest = kafkaRecord.value();
        if (friendRequest == null) {
            log.error("Friend request event with key " + kafkaRecord.key() + " is empty");
            return;
        }

        if (friendRequest.getRequestStatus() == null) {
            log.error("Friend request status is empty in event " + kafkaRecord.key());
            return;
        }

        if (friendRequest.getRequestStatus() == FriendRequestStatus.ACCEPTED) {
            Long senderId = friendRequest.getSenderId();
            Long receiverId = friendRequest.getReceiverId();

            UserNode senderNode = userNodeRepository.findById(senderId).orElseThrow(UserNotFoundException::new);
            UserNode receiverNode = userNodeRepository.findById(receiverId).orElseThrow(UserNotFoundException::new);

            senderNode.addFriend(receiverNode);
            userNodeRepository.save(senderNode);
        }
    }

    @Override
    @Transactional(value = "neo4jTransactionManager")
    public boolean unfriend(Long friendId) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        boolean isRemoved = removeFriend(authenticatedUserId, friendId);
        if (isRemoved) {
            updateNewsfeedFor(authenticatedUserId, friendId);
        }
        return isRemoved;
    }

    @KafkaListener(topics = "unfriend")
    @Transactional(value = "neo4jTransactionManager")
    public void unfriend(ConsumerRecord<String, Map<String, Long>> consumerRecord) {
        Map<String, Long> values = consumerRecord.value();

        if (values == null || values.isEmpty())
            return;

        final String TARGET_USER_KEY = "targetUserId";
        final String SOURCE_USER_KEY = "sourceUserId";

        Long targetUserId = ((Number) values.get(TARGET_USER_KEY)).longValue();
        Long sourceUserId = ((Number) values.get(SOURCE_USER_KEY)).longValue();

        if (targetUserId == null || sourceUserId == null)
            return;

        boolean isRemoved = removeFriend(targetUserId, sourceUserId);

        if (isRemoved)
            updateNewsfeedFor(targetUserId, sourceUserId);
    }

    private boolean removeFriend(Long userId, Long friendId) {
        UserNode user = userNodeRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        UserNode friend = userNodeRepository.findById(friendId).orElseThrow(UserNotFoundException::new);

        boolean isRemovedFromUserList = user.getFriends().removeIf(u -> u.getUserId().equals(friend.getUserId()));
        if (isRemovedFromUserList) {
            userNodeRepository.save(user);
            return true;
        }

        boolean isRemovedFromFriendList = friend.getFriends().removeIf(u -> u.getUserId().equals(user.getUserId()));
        if (isRemovedFromFriendList) {
            userNodeRepository.save(friend);
            return true;
        }

        return false;
    }

    private void updateNewsfeedFor(Long targetUserId, Long sourceUserId) {
        final String TARGET_USER_KEY = "targetUserId";
        final String SOURCE_USER_KEY = "sourceUserId";

        kafkaTemplate.send("newsfeed-following-removal",
                Map.of(TARGET_USER_KEY, targetUserId, SOURCE_USER_KEY, sourceUserId));
        kafkaTemplate.send("newsfeed-following-removal",
                Map.of(TARGET_USER_KEY, sourceUserId, SOURCE_USER_KEY, targetUserId));
    }

    @Override
    public List<User> findMyFriends(PaginationInfo paginationInfo) {
        SecurityUtils.throwIfNotAuthenticated();
        Long userId = SecurityUtils.getAuthenticatedUserId();

        Integer offset = (paginationInfo.getPage() - 1) * paginationInfo.getPageSize();
        List<UserNode> friends = userNodeRepository.findFriends(userId, offset, paginationInfo.getPageSize());
        if (friends == null || friends.isEmpty())
            return new ArrayList<>();

        List<Long> friendsIds = friends.stream()
                .map(f -> f.getUserId())
                .distinct()
                .toList();
        return userRepository.findAllById(friendsIds);
    }

    @Override
    public List<User> findFriends(Long userId, PaginationInfo paginationInfo) {
        Integer offset = (paginationInfo.getPage() - 1) * paginationInfo.getPageSize();
        List<UserNode> friends = userNodeRepository.findFriends(userId, offset, paginationInfo.getPageSize());
        if (friends == null || friends.isEmpty())
            return new ArrayList<>();

        List<Long> friendsIds = friends.stream()
                .map(f -> f.getUserId())
                .distinct()
                .toList();
        return userRepository.findAllById(friendsIds);
    }

    @Override
    public boolean isFriend(Long friendId) {
        if (friendId == null)
            throw new InvalidInputException("Friend id cannot be empty");
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        if (authenticatedUserId.equals(friendId))
            throw new InvalidInputException("Authenticated user and friend are the same");
        return userNodeRepository.isFriend(authenticatedUserId, friendId);
    }

    @Override
    public Long countFriends(Long userId) {
        if (userId == null)
            throw new InvalidInputException("User ID cannot be empty");

        SecurityUtils.throwIfNotAuthenticated();
        return userNodeRepository.countFriends(userId);
    }

    @Override
    public List<User> findRecommendedConnections(PaginationInfo paginationInfo) {
        SecurityUtils.throwIfNotAuthenticated();
        Long userId = SecurityUtils.getAuthenticatedUserId();

        Integer offset = (paginationInfo.getPage() - 1) * paginationInfo.getPageSize();
        List<UserNode> connections = userNodeRepository.findRecommendedConnections(userId, offset,
                paginationInfo.getPageSize());
        if (connections == null || connections.isEmpty())
            return new ArrayList<>();

        List<Long> connectionsIds = connections.stream()
                .map(f -> f.getUserId())
                .distinct()
                .toList();
        return userRepository.findAllById(connectionsIds);
    }

}
