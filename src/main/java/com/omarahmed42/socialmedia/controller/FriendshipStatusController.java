package com.omarahmed42.socialmedia.controller;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.dto.response.FriendshipStatus;
import com.omarahmed42.socialmedia.service.FriendshipStatusService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class FriendshipStatusController {

    private final FriendshipStatusService friendshipStatusService;

    @QueryMapping
    public FriendshipStatus getFriendshipStatus(@Argument Long friendId) {
        return friendshipStatusService.getFriendshipStatus(friendId);
    }
}
