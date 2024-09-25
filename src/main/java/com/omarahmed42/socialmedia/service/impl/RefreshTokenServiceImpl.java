package com.omarahmed42.socialmedia.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.builder.Token;
import com.omarahmed42.socialmedia.enums.TokenStatus;
import com.omarahmed42.socialmedia.model.RefreshToken;
import com.omarahmed42.socialmedia.repository.RefreshTokenRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    public void storeToken(Token token, Long userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.fromString(token.getId()));
        refreshToken.setStatus(TokenStatus.VALID);
        refreshToken.setValidUntil(token.getExpiration().getTime());
        refreshToken.setUser(userRepository.getReferenceById(userId));

        refreshTokenRepository.save(refreshToken);
    }

}
