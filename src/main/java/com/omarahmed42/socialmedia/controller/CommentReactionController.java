package com.omarahmed42.socialmedia.controller;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.model.Reaction;
import com.omarahmed42.socialmedia.service.CommentReactionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CommentReactionController {

    private final CommentReactionService commentReactionService;

    @MutationMapping
    public Boolean saveCommentReaction(@Argument Integer reactionId, @Argument Long commentId) {
        return commentReactionService.saveCommentReaction(reactionId, commentId) == null;
    }

    @MutationMapping
    public Boolean removeCommentReaction(@Argument Long commentId) {
        return commentReactionService.removeCommentReaction(commentId) == null;
    }

    @QueryMapping
    public Reaction getCommentReaction(@Argument Long commentId) {
        return commentReactionService.getCommentReaction(commentId);
    }
}
