package com.omarahmed42.socialmedia.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.model.FriendRequest;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.service.FriendRequestService;
import com.omarahmed42.socialmedia.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendRequestService friendRequestService;
    private final UserService userService;

    @MutationMapping
    public FriendRequest sendFriendRequest(@Argument Long receiverId) {
        return friendRequestService.sendFriendRequest(receiverId);
    }

    @MutationMapping
    public Boolean cancelFriendRequest(@Argument Long id) {
        return friendRequestService.cancelFriendRequest(id);
    }

    @MutationMapping
    public Boolean acceptFriendRequest(@Argument Long id) {
        return friendRequestService.acceptFriendRequest(id);
    }

    @MutationMapping
    public Boolean rejectFriendRequest(@Argument Long id) {
        return friendRequestService.rejectFriendRequest(id);
    }

    @SchemaMapping(typeName = "FriendRequest", field = "sender")
    public User sender(FriendRequest friendRequest) {
        return userService.getUser(friendRequest.getSender().getId());
    }

    @SchemaMapping(typeName = "FriendRequest", field = "receiver")
    public User receiver(FriendRequest friendRequest) {
        return userService.getUser(friendRequest.getReceiver().getId());
    }

    @QueryMapping
    public List<FriendRequest> findFriendRequests(@Argument boolean isSender, @Argument Integer page,
            @Argument Integer pageSize,
            @Argument Long after, @Argument Long before) {
        return friendRequestService.findFriendRequests(isSender, new PaginationInfo(page, pageSize), after, before);
    }
}
