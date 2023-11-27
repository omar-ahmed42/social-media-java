package com.omarahmed42.socialmedia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.User;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Cacheable(cacheNames = "posts", key = "#id", condition = "#id != null")
    Optional<Post> findById(Long id);

    @CachePut(cacheNames = "posts", key = "#post.id")
    Post save(Post post);

    int deleteByIdAndUserId(Long postId, Long userId);

    @Query(value = "SELECT c FROM Comment c WHERE c.post.id = :post_id")
    List<Comment> findAllCommentsById(@Param("post_id") Long postId);

    List<Post> findAllByUser(User user);
}
