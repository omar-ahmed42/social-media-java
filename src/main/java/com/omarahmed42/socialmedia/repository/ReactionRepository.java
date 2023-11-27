package com.omarahmed42.socialmedia.repository;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.Reaction;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Integer> {
    
    @Cacheable(cacheNames = "reactions", key = "#id")
    Optional<Reaction> findById(Integer id);
}
