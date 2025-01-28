package com.omarahmed42.socialmedia.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.dto.projection.AttachmentDto;
import com.omarahmed42.socialmedia.dto.response.ReactionStatistics;
import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.PostAttachment;
import com.omarahmed42.socialmedia.model.PostAttachmentId;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.projection.PostInputProjection;
import com.omarahmed42.socialmedia.service.PostAttachmentService;
import com.omarahmed42.socialmedia.service.PostService;
import com.omarahmed42.socialmedia.service.StatisticsService;
import com.omarahmed42.socialmedia.service.UserService;

import graphql.GraphQLContext;

@Controller
public class PostController {

    private final PostService postService;
    private final PostAttachmentService postAttachmentService;
    private final UserService userService;
    private final StatisticsService statisticsService;

    public PostController(PostService postService, UserService userService,
            @Qualifier("postReactionsStatisticsService") StatisticsService statisticsService, PostAttachmentService postAttachmentService) {
        this.postService = postService;
        this.userService = userService;
        this.statisticsService = statisticsService;
        this.postAttachmentService = postAttachmentService;
    }

    @QueryMapping
    public Post findPost(@Argument Long id, GraphQLContext context) {
        return postService.findPost(id);
    }

    @MutationMapping
    public Post savePost(@Argument(name = "postInput") PostInputProjection postInputProjection) {
        return postService.addPost(postInputProjection);
    }

    @MutationMapping
    public Integer deletePost(@Argument Long id) {
        return postService.deletePost(id);
    }

    @QueryMapping
    public List<Post> findPostsByUserId(@Argument Long userId, @Argument Integer page, @Argument Integer pageSize, @Argument Long lastSeenPostId) {
        return postService.findPostsByUserId(userId, new PaginationInfo(page, pageSize), lastSeenPostId);
    }

    @SchemaMapping(typeName = "Post", field = "user")
    public User user(Post post) {
        return userService.getUser(post.getUser().getId());
    }

    @SchemaMapping(typeName = "Post", field = "comments")
    public List<Comment> comments(Post post) {
        return postService.getCommentsBy(post);
    }

    @SchemaMapping(typeName = "Post", field = "postAttachments")
    public List<AttachmentDto> postAttachments(Post post) {
        List<PostAttachment> postAttachments = postAttachmentService.findPostAttachmentsByPost(post);
        return postAttachments.stream().map(PostAttachment::getPostAttachmentId).map(PostAttachmentId::getAttachment).map(attachment -> new AttachmentDto(attachment.getId(), attachment.getUrl(), attachment.getAttachmentType())).toList();
    }

    @SchemaMapping(typeName = "Post", field = "reactionStatistics")
    public ReactionStatistics reactionStatistics(Post post) {
        return (ReactionStatistics) statisticsService.getStatistics(post.getId().toString());
    }
}
