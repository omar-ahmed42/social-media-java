package com.omarahmed42.socialmedia.controller;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.datastax.oss.driver.shaded.guava.common.collect.Sets;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.service.PostService;
import com.omarahmed42.socialmedia.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;

    @MutationMapping
    public User addUser(@Argument String firstName, @Argument String lastName, @Argument String email,
            @Argument String password, @Argument LocalDate dateOfBirth,
            @Argument List<String> roles) {
        return userService.addUser(firstName, lastName, email, password, dateOfBirth, true, true, new HashSet<>(roles));
    }

    @SchemaMapping(typeName = "User", field = "posts")
    public List<Post> posts(User user) {
        return postService.getPostsBy(user);
    }

    @SchemaMapping(typeName = "User", field = "roles")
    public Set<Role> roles(User user) {
        return Sets.newHashSet(); // TODO: Add business logic
    }

}
