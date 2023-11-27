package com.omarahmed42.socialmedia.service;

import java.util.List;

import com.omarahmed42.socialmedia.model.User;

public interface BlockingService {
    boolean blockUser(Long blockedUserId);

    boolean unblockUser(Long blockedUserId);

    boolean isBlocked(Long blocker, Long blocked);

    List<User> findAllBlockedUsers();
}
