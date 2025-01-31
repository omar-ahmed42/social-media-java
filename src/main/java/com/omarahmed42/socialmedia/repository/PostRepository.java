package com.omarahmed42.socialmedia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.enums.PostStatus;
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

    @Query(value = "SELECT * FROM post WHERE user_id = :user_id AND id > :last_seen_post_id ORDER BY id DESC LIMIT :page_size", nativeQuery = true)
    List<Post> findAllByUserIdAndLastSeenPostId(@Param("user_id") Long userId, @Param("last_seen_post_id") Long lastSeenPostId, @Param("page_size") Integer pageSize);

    List<Post> findAllByUser_Id(Long userId, Pageable page);

    @Query(value = "SELECT p FROM Post p WHERE p.user.id = :userId AND p.id > :lastSeenPostId AND p.postStatus = :postStatus ORDER BY p.id DESC LIMIT :pageSize")
    List<Post> findAllByUserIdAndLastSeenPostIdAndPostStatus(@Param("userId") Long userId, @Param("lastSeenPostId") Long lastSeenPostId, @Param("postStatus") PostStatus postStatus, @Param("pageSize") Integer pageSize);
    
    List<Post> findAllByUserIdAndPostStatus(Long userId, PostStatus postStatus, Pageable page);

	List<Post> findAllByUserId(Long userId, Pageable page);

    boolean existsByParentId(Long postId);
}
