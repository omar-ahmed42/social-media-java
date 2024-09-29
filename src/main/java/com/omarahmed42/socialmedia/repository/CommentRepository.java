package com.omarahmed42.socialmedia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.CommentAttachment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    @EntityGraph(attributePaths = { "post" })
    Optional<Comment> findCommentById(Long commentId);

    @Cacheable(cacheNames = "comments", key = "#id")
    Optional<Comment> findById(Long id);

    @CachePut(cacheNames = "comments", key = "#comment.id", condition = "#comment != null && #comment.id != null")
    Comment save(Comment comment);

    List<CommentAttachment> findCommentAttachmentsById(Long commentId);
}
