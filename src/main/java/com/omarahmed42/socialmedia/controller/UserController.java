package com.omarahmed42.socialmedia.controller;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.datastax.oss.driver.shaded.guava.common.collect.Sets;
import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.model.Attachment;
import com.omarahmed42.socialmedia.service.PostService;
import com.omarahmed42.socialmedia.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;

    @MutationMapping
    public User addUser(@Argument String firstName, @Argument String lastName, @Argument String email, @Argument String username,
            @Argument String password, @Argument LocalDate dateOfBirth,
            @Argument List<String> roles) {
        return userService.addUser(firstName, lastName, username, email, password, dateOfBirth, true, true, new HashSet<>(roles));
    }

    @QueryMapping
    public User findUserPersonalInfo(@Argument Long userId) {
        return userService.getUserPersonalInfo(userId);
    }

    @QueryMapping
    public User findUserPublicInfo(@Argument Long userId) {
        return userService.getUserPublicInfo(userId);
    }

    @SchemaMapping(typeName = "User", field = "posts")
    public List<Post> posts(User user) {
        return postService.findPostsByUserId(user.getId(), new PaginationInfo(1, 15), null);
    }
    
    @SchemaMapping(typeName = "User", field = "roles")
    public Set<Role> roles(User user) {
        return Sets.newHashSet(); // TODO: Add business logic
    }

    @PutMapping("/api/v1/users/me/avatar")
    @ResponseBody
    public ResponseEntity<Void> updateAvatar(MultipartFile avatarFile) {
        userService.updateAvatar(avatarFile);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/v1/users/me/cover")
    @ResponseBody
    public ResponseEntity<Void> updateCover(MultipartFile coverFile) {
        userService.updateCover(coverFile);
        return ResponseEntity.ok().build();
    }

    @SchemaMapping(typeName = "User", field = "avatar")
    public Attachment avatar(User user) {
        return user.getAvatar();
    }
    
    @SchemaMapping(typeName = "User", field = "coverPicture")
    public Attachment coverPicture(User user) {
        return user.getCoverPicture();
    }
}
