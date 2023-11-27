package com.omarahmed42.socialmedia.repository;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.CommentReaction;
import com.omarahmed42.socialmedia.model.CommentReactionId;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, CommentReactionId> {

    @Cacheable(cacheNames = "comment-reactions", key = "#id")
    Optional<CommentReaction> findById(CommentReactionId id);

    Long countByReactionName(String reactionName);
}
