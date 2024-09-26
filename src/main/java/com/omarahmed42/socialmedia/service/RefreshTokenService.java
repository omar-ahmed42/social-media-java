package com.omarahmed42.socialmedia.service;

import com.omarahmed42.socialmedia.builder.Token;
import com.omarahmed42.socialmedia.dto.response.Jwt;
import com.omarahmed42.socialmedia.dto.response.JwtResponse;

public interface RefreshTokenService {
    void storeToken(Token token, Long userId);
    JwtResponse refreshToken(Jwt refreshToken);
}
