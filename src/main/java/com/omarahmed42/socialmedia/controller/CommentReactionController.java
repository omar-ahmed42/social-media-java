package com.omarahmed42.socialmedia.controller;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.service.CommentReactionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CommentReactionController {

    private final CommentReactionService postReactionService;

    @MutationMapping
    public Boolean saveCommentReaction(@Argument Integer reactionId, @Argument Long commentId) {
        return postReactionService.saveCommentReaction(reactionId, commentId) == null;
    }
}
