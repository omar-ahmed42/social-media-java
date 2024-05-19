package com.omarahmed42.socialmedia.service;

import java.util.List;

import com.omarahmed42.socialmedia.dto.PaginationInfo;
import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.projection.PostInputProjection;

public interface PostService {
    Post addPost(PostInputProjection postInputProjection);

    Integer deletePost(Long postId);

    Post findPost(Long postId);

    List<Comment> getCommentsBy(Post post);

    List<Post> getPostsBy(User user);

    List<Post> findPostsByUserId(Long userId, PaginationInfo pageInfo, Long lastSeenPostId);
}
