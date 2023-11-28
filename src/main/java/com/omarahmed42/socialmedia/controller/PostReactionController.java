package com.omarahmed42.socialmedia.controller;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.service.PostReactionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PostReactionController {

    private final PostReactionService postReactionService;

    @MutationMapping
    public Boolean savePostReaction(@Argument Integer reactionId, @Argument Long postId) {
        return postReactionService.savePostReaction(reactionId, postId) == null;
    }
}
