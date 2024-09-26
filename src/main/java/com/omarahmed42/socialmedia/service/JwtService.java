package com.omarahmed42.socialmedia.service;

import com.omarahmed42.socialmedia.builder.Token;
import com.omarahmed42.socialmedia.dto.response.Jwt;
import com.omarahmed42.socialmedia.dto.response.JwtResponse;

public interface JwtService {
    String extractUserName(String token);

    boolean isTokenValid(String token);

    Jwt generateToken(Token token);

    JwtResponse generateTokens(String subject);

    Token parse(String jwt);

}
