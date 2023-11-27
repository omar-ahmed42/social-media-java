package com.omarahmed42.socialmedia.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.dto.event.FriendRequestEvent;
import com.omarahmed42.socialmedia.enums.FriendRequestStatus;
import com.omarahmed42.socialmedia.exception.ForbiddenFriendRequestAccessException;
import com.omarahmed42.socialmedia.exception.FriendRequestNotFoundException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.model.FriendRequest;
import com.omarahmed42.socialmedia.model.FriendRequest_;
import com.omarahmed42.socialmedia.repository.FriendRequestRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.service.BlockingService;
import com.omarahmed42.socialmedia.service.FriendRequestService;
import com.omarahmed42.socialmedia.service.FriendService;
import com.omarahmed42.socialmedia.specification.FriendRequestSpecification;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendRequestServiceImpl implements FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendService friendService;
    private final BlockingService blockingService;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public boolean acceptFriendRequest(Long id) {
        SecurityUtils.throwIfNotAuthenticated();
        FriendRequest friendRequest = friendRequestRepository.findById(id)
                .orElseThrow(FriendRequestNotFoundException::new);

        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        throwIfNotReceiver(friendRequest, "accept", authenticatedUserId);

        if (friendRequest.getRequestStatus() == FriendRequestStatus.PENDING) {
            friendRequest.setRequestStatus(FriendRequestStatus.ACCEPTED);
            friendRequest = friendRequestRepository.save(friendRequest);
            establishFriendship(friendRequest);
            return true;
        }

        return friendRequest.getRequestStatus() == FriendRequestStatus.ACCEPTED;
    }

    private void establishFriendship(FriendRequest friendRequest) {
        String eventId = UUID.randomUUID().toString();
        FriendRequestEvent friendRequestEvent = new FriendRequestEvent(friendRequest.getId(),
                friendRequest.getSender().getId(), friendRequest.getReceiver().getId(),
                friendRequest.getRequestStatus());
        kafkaTemplate.send("friend-request", eventId, friendRequestEvent);
    }

    @Override
    public boolean rejectFriendRequest(Long id) {
        SecurityUtils.throwIfNotAuthenticated();
        FriendRequest friendRequest = friendRequestRepository.findById(id)
                .orElseThrow(FriendRequestNotFoundException::new);

        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        throwIfNotReceiver(friendRequest, "reject", authenticatedUserId);

        if (friendRequest.getRequestStatus() == FriendRequestStatus.PENDING) {
            friendRequest.setRequestStatus(FriendRequestStatus.REJECTED);
            friendRequestRepository.save(friendRequest);
            return true;
        }

        return friendRequest.getRequestStatus() == FriendRequestStatus.REJECTED;
    }

    private void throwIfNotReceiver(FriendRequest friendRequest, String operation, Long userId) {
        Long receiverId = friendRequest.getReceiver().getId();
        if (!receiverId.equals(userId))
            throw new ForbiddenFriendRequestAccessException(
                    "Forbidden: cannot " + operation + " friend request with id " + friendRequest.getId());
    }

    @Override
    public FriendRequest sendFriendRequest(Long receiverId) {
        SecurityUtils.throwIfNotAuthenticated();
        Long senderId = SecurityUtils.getAuthenticatedUserId();
        if (senderId.equals(receiverId))
            throw new InvalidInputException("Sender and Receiver cannot be the same");

        if (areFriends(senderId, receiverId) || isBlocked(senderId, receiverId)) {
            return null;
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(userRepository.getReferenceById(senderId));
        friendRequest.setReceiver(userRepository.getReferenceById(receiverId));
        friendRequest.setRequestStatus(FriendRequestStatus.PENDING);

        friendRequest = friendRequestRepository.save(friendRequest);
        return friendRequest;
    }

    private boolean isBlocked(Long blockerId, Long blockedUserId) {
        return blockingService.isBlocked(blockerId, blockedUserId);
    }

    private boolean areFriends(Long senderId, Long receiverId) {
        return friendService.isFriend(senderId, receiverId);
    }

    @Override
    public boolean cancelFriendRequest(Long id) {
        SecurityUtils.throwIfNotAuthenticated();

        FriendRequest friendRequest = friendRequestRepository.findById(id)
                .orElseThrow(FriendRequestNotFoundException::new);

        throwIfNotSender(friendRequest, "cancel", SecurityUtils.getAuthenticatedUserId());

        if (friendRequest.getRequestStatus() == FriendRequestStatus.PENDING) {
            friendRequest.setRequestStatus(FriendRequestStatus.CANCELLED);
            friendRequestRepository.save(friendRequest);
            return true;
        }
        return friendRequest.getRequestStatus() == FriendRequestStatus.CANCELLED;
    }

    private void throwIfNotSender(FriendRequest friendRequest, String operation, Long userId) {
        Long senderId = friendRequest.getSender().getId();
        if (!senderId.equals(userId))
            throw new ForbiddenFriendRequestAccessException(
                    "Forbidden: cannot " + operation + " friend request with id " + friendRequest.getId());
    }

    public List<FriendRequest> findFriendRequests(boolean isSender, PaginationInfo paginationInfo,
            Long after, Long before) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
        
        Specification<FriendRequest> specs = FriendRequestSpecification.hasRequestStatus(FriendRequestStatus.PENDING);
        if (isSender) {
            specs = specs.and(FriendRequestSpecification.hasSenderId(authenticatedUserId));
        } else {
            specs = specs.and(FriendRequestSpecification.hasReceiverId(authenticatedUserId));
        }

        Pageable pageable = null;
        if (after != null) {
            specs = specs.and(FriendRequestSpecification.afterId(after));
            pageable = PageRequest.of(paginationInfo.getPage() - 1, paginationInfo.getPageSize(),
                    Sort.by(Direction.ASC, FriendRequest_.ID));

        } else if (before != null) {
            specs = specs.and(FriendRequestSpecification.beforeId(before));
            pageable = PageRequest.of(paginationInfo.getPage() - 1, paginationInfo.getPageSize(),
                    Sort.by(Direction.DESC, FriendRequest_.ID));
        }

        return friendRequestRepository.findAll(specs, pageable);
    }
}
