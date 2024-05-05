package com.omarahmed42.socialmedia.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.service.FanoutService;

import graphql.GraphQLContext;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class NewsfeedController {

    private final FanoutService fanoutService;

    @QueryMapping
    public List<Post> fetchNewsfeed(GraphQLContext context) {
        return fanoutService.getNewsfeed();
    }
}
