package com.omarahmed42.socialmedia.service.impl;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omarahmed42.socialmedia.dto.response.FriendshipStatus;
import com.omarahmed42.socialmedia.enums.FriendRequestStatus;
import com.omarahmed42.socialmedia.model.FriendRequest;
import com.omarahmed42.socialmedia.repository.FriendRequestRepository;
import com.omarahmed42.socialmedia.repository.graph.UserNodeRepository;
import com.omarahmed42.socialmedia.service.AsyncService;
import com.omarahmed42.socialmedia.service.FriendshipStatusService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import graphql.com.google.common.base.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FriendshipStatusServiceImpl implements FriendshipStatusService {

    private final AsyncService asyncService;
    private final FriendRequestRepository friendRequestRepository;
    private final UserNodeRepository userNodeRepository;

    @Override
    @Transactional(readOnly = true)
    public FriendshipStatus getFriendshipStatus(Long friendId) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authUserId = SecurityUtils.getAuthenticatedUserId();

        CompletableFuture<Boolean> isFriendFuture = asyncService.getCompletable(
                () -> CompletableFuture.completedFuture(userNodeRepository.isFriend(authUserId, friendId)));

        CompletableFuture<Optional<FriendRequest>> pendingRequestFuture = asyncService
                .getCompletable(() -> CompletableFuture
                        .completedFuture(friendRequestRepository.findByFriendRequestStatus(authUserId, friendId,
                                FriendRequestStatus.PENDING)));

        CompletableFuture.allOf(isFriendFuture, pendingRequestFuture).join();

        Optional<FriendRequest> maybePendingRequest = pendingRequestFuture.join();

        Long incomingRequest = null;
        Long outgoingRequest = null;
        if (!maybePendingRequest.isEmpty()) {
            FriendRequest friendRequest = maybePendingRequest.get();
            if (isSender(friendRequest, authUserId))
                outgoingRequest = friendRequest.getId();
            else
                incomingRequest = friendRequest.getId();
        }

        Boolean isFriend = isFriendFuture.join();

        return new FriendshipStatus(isFriend, incomingRequest, outgoingRequest);
    }

    private boolean isSender(FriendRequest friendRequest, Long userId) {
        return Objects.equal(friendRequest.getSender().getId(), userId);
    }

}
