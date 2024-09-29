package com.omarahmed42.socialmedia.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.dto.response.ReactionStatistics;
import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.CommentAttachment;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.projection.CommentInputProjection;
import com.omarahmed42.socialmedia.service.CommentService;
import com.omarahmed42.socialmedia.service.StatisticsService;
import com.omarahmed42.socialmedia.service.UserService;

@Controller
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;
    private final StatisticsService statisticsService;

    public CommentController(CommentService commentService, UserService userService,
            @Qualifier("commentReactionsStatisticsService") StatisticsService statisticsService) {
        this.commentService = commentService;
        this.userService = userService;
        this.statisticsService = statisticsService;
    }

    @QueryMapping
    public Comment findComment(@Argument Long id) {
        return commentService.findComment(id);
    }

    @MutationMapping
    public Comment saveComment(@Argument(name = "commentInput") CommentInputProjection commentInputProjection) {
        return commentService.saveComment(commentInputProjection);
    }

    @MutationMapping
    public Boolean deleteComment(@Argument Long id) {
        return commentService.deleteComment(id);
    }

    @SchemaMapping(typeName = "Comment", field = "post")
    public Post post(Comment comment) {
        return commentService.getPostBy(comment);
    }

    @SchemaMapping(typeName = "Comment", field = "user")
    public User user(Comment comment) {
        return userService.getUser(comment.getUser().getId());
    }

    @SchemaMapping(typeName = "Comment", field = "commentAttachments")
    public List<CommentAttachment> commentAttachments(Comment comment) {
        return commentService.getCommentAttachmentsBy(comment);
    }

    @SchemaMapping(typeName = "Comment", field = "reactionStatistics")
    public ReactionStatistics reactionStatistics(Comment comment) {
        return (ReactionStatistics) statisticsService.getStatistics(comment.getId().toString());
    }

    @QueryMapping
    public List<Comment> getCommentsByPostId(@Argument Long postId, @Argument Integer page, @Argument Integer pageSize,
            @Argument Long after) {
        return commentService.getCommentsByPostId(postId, new PaginationInfo(page, pageSize), after);
    }
}
