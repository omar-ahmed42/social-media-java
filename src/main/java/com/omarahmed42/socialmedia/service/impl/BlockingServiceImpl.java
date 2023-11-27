package com.omarahmed42.socialmedia.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omarahmed42.socialmedia.exception.UserNotFoundException;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.model.graph.UserNode;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.repository.graph.UserNodeRepository;
import com.omarahmed42.socialmedia.service.BlockingService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlockingServiceImpl implements BlockingService {

    private final UserNodeRepository userNodeRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public boolean isBlocked(Long firstUser, Long secondUser) {
        return userNodeRepository.isBlocked(firstUser, secondUser);
    }

    @Override
    public boolean blockUser(Long blockedUserId) {
        SecurityUtils.throwIfNotAuthenticated();
        Long blockerId = SecurityUtils.getAuthenticatedUserId();
        UserNode blocker = userNodeRepository.findById(blockerId).orElseThrow(UserNotFoundException::new);
        UserNode userToBeBlocked = userNodeRepository.findById(blockedUserId)
                .orElseThrow(UserNotFoundException::new);
        blocker.addBlockedUser(userToBeBlocked);
        userNodeRepository.save(blocker);
        removeIfFriend(blocker, userToBeBlocked);
        return true;
    }

    private void removeIfFriend(UserNode blocker, UserNode blocked) {
        kafkaTemplate.send("unfriend",
                Map.of("targetUserId", blocker.getUserId(), "sourceUserId", blocked.getUserId()));
    }

    @Override
    @Transactional(transactionManager = "neo4jTransactionManager")
    public boolean unblockUser(Long blockedUserId) {
        SecurityUtils.throwIfNotAuthenticated();
        Long blockerId = SecurityUtils.getAuthenticatedUserId();
        userNodeRepository.deleteBlocksRelationshipBetween(blockerId, blockedUserId);
        return true;
    }

    @Override
    public List<User> findAllBlockedUsers() {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

        List<UserNode> blockedUsers = userNodeRepository.findAllBlockedUsersBy(authenticatedUserId);
        if (blockedUsers == null || blockedUsers.isEmpty())
            return new ArrayList<>();

        List<Long> blockedUserIds = blockedUsers.stream().map(b -> b.getUserId()).distinct().toList();
        return userRepository.findAllById(blockedUserIds);
    }
}
