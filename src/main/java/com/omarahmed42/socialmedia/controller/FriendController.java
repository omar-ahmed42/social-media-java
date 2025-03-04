package com.omarahmed42.socialmedia.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.service.FriendService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @MutationMapping
    public Boolean unfriend(@Argument Long friendId) {
        return friendService.unfriend(friendId);
    }

    @QueryMapping
    public List<User> findMyFriends(@Argument Integer page, @Argument Integer pageSize) {
        return friendService.findMyFriends(new PaginationInfo(page, pageSize));
    }

    @QueryMapping
    public List<User> findFriends(@Argument Long userId, @Argument Integer page, @Argument Integer pageSize) {
        return friendService.findFriends(userId, new PaginationInfo(page, pageSize));
    }

    @QueryMapping
    public Boolean isFriend(@Argument Long friendId) {
        return friendService.isFriend(friendId);
    }

    @QueryMapping
    public Long countFriends(@Argument Long userId) {
        return friendService.countFriends(userId);
    }

    @QueryMapping
    public List<User> findRecommendedConnections(@Argument Integer page, @Argument Integer pageSize) {
        return friendService.findRecommendedConnections(new PaginationInfo(page, pageSize));
    }
}
