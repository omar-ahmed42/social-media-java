package com.omarahmed42.socialmedia.service;

import com.omarahmed42.socialmedia.dto.response.FriendshipStatus;

public interface FriendshipStatusService {
    FriendshipStatus getFriendshipStatus(Long friendId);
}
