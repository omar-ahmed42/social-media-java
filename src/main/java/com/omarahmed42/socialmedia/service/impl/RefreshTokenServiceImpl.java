package com.omarahmed42.socialmedia.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.omarahmed42.socialmedia.builder.Token;
import com.omarahmed42.socialmedia.dto.response.Jwt;
import com.omarahmed42.socialmedia.dto.response.JwtResponse;
import com.omarahmed42.socialmedia.enums.TokenStatus;
import com.omarahmed42.socialmedia.exception.InternalServerErrorException;
import com.omarahmed42.socialmedia.exception.RefreshTokenExpiredException;
import com.omarahmed42.socialmedia.exception.TokenDeniedException;
import com.omarahmed42.socialmedia.model.RefreshToken;
import com.omarahmed42.socialmedia.repository.RefreshTokenRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.service.JwtService;
import com.omarahmed42.socialmedia.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public void storeToken(Token token, Long userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.fromString(token.getId()));
        refreshToken.setStatus(TokenStatus.VALID);
        refreshToken.setValidUntil(token.getExpiration().getTime());
        refreshToken.setUser(userRepository.getReferenceById(userId));
        refreshToken.markNew();

        refreshTokenRepository.save(refreshToken);
    }

    @Override
    public JwtResponse refreshToken(Jwt refreshToken) {
        String tokenStr = refreshToken.token();
        if (!jwtService.isTokenValid(tokenStr)) {
            throw new RefreshTokenExpiredException("Refresh token expired");
        }

        Token parsedToken = jwtService.parse(tokenStr);
        String jti = parsedToken.getId();
        RefreshToken retrievedRefreshToken = refreshTokenRepository.findById(UUID.fromString(jti))
                .orElseThrow(() -> new TokenDeniedException("Access denied"));

        switch (retrievedRefreshToken.getStatus()) {
            case INVALID:
            case COMPROMISED:
                throw new TokenDeniedException("Access Denied");
            case CONSUMED:
                Long userId = retrievedRefreshToken.getUser().getId();
                compromiseTokensFor(userId);
                throw new TokenDeniedException("Access Denied");
            case VALID:
                return rotate(retrievedRefreshToken);
            default:
                throw new InternalServerErrorException();
        }
    }

    private void compromiseTokensFor(Long userId) {
        List<RefreshToken> refreshTokens = refreshTokenRepository.findAllByUser_idAndStatus(userId, TokenStatus.VALID);
        if (refreshTokens == null || refreshTokens.isEmpty())
            return;

        for (RefreshToken token : refreshTokens) {
            token.setStatus(TokenStatus.COMPROMISED);
        }

        refreshTokenRepository.saveAll(refreshTokens);
    }

    private JwtResponse rotate(RefreshToken refreshToken) {
        JwtResponse jwtResponse = jwtService.generateTokens(refreshToken.getUser().getEmail());
        transactionTemplate.executeWithoutResult((status) -> {
            refreshToken.setStatus(TokenStatus.CONSUMED);
            storeToken(jwtService.parse(jwtResponse.getRefreshToken()), refreshToken.getUser().getId());
            refreshTokenRepository.save(refreshToken);
        });
        return jwtResponse;
    }

}
