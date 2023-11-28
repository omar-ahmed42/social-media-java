package com.omarahmed42.socialmedia.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.service.BlockingService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BlockingController {

    private final BlockingService blockingService;

    @MutationMapping
    public Boolean blockUser(@Argument Long userToBeBlockedId) {
        return blockingService.blockUser(userToBeBlockedId);
    }

    @MutationMapping
    public boolean unblockUser(@Argument Long userToBeUnBlockedId) {
        return blockingService.unblockUser(userToBeUnBlockedId);
    }

    @QueryMapping
    public List<User> findAllBlockedUsers() {
        return blockingService.findAllBlockedUsers();
    }
}
