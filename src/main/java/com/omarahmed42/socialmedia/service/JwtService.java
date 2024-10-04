package com.omarahmed42.socialmedia.service;

import com.omarahmed42.socialmedia.builder.Token;
import com.omarahmed42.socialmedia.dto.response.Jwt;
import com.omarahmed42.socialmedia.dto.response.JwtResponse;

public interface JwtService {
    Long extractSubject(String token);

    String extractUserName(String token);

    boolean isTokenValid(String token);

    Jwt generateToken(Token token);

    JwtResponse generateTokens(String subject, String username);

    Token parse(String jwt);

}
