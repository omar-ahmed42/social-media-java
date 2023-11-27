package com.omarahmed42.socialmedia.service;

import java.util.List;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.model.FriendRequest;

public interface FriendRequestService {
    boolean acceptFriendRequest(Long id);

    boolean rejectFriendRequest(Long id);

    FriendRequest sendFriendRequest(Long receiverId);

    boolean cancelFriendRequest(Long id);

    List<FriendRequest> findFriendRequests(boolean isSender, PaginationInfo paginationInfo,
            Long after, Long before);
}
