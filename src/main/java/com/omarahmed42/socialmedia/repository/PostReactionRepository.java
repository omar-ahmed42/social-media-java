package com.omarahmed42.socialmedia.repository;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.PostReaction;
import com.omarahmed42.socialmedia.model.PostReactionId;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, PostReactionId> {

    @Cacheable(cacheNames = "post-reactions", key = "#id")
    Optional<PostReaction> findById(PostReactionId id);

    Long countByReactionName(String reactionName);
}
