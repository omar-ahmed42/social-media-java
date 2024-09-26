package com.omarahmed42.socialmedia.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.enums.TokenStatus;
import com.omarahmed42.socialmedia.model.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    List<RefreshToken> findAllByUser_idAndStatus(Long userId, TokenStatus status);
}
