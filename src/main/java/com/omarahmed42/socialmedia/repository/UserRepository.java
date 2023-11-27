package com.omarahmed42.socialmedia.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = { "roles" })
    Optional<User> findByEmail(String username);

    boolean existsByEmail(String email);

}
