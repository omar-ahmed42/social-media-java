package com.omarahmed42.socialmedia.service;

import org.springframework.security.core.userdetails.UserDetails;

import com.omarahmed42.socialmedia.builder.Token;
import com.omarahmed42.socialmedia.dto.response.Jwt;

public interface JwtService {
    String extractUserName(String token);

    String generateToken(UserDetails userDetails);

    String generateToken(UserDetails userDetails, Long expiration);

    boolean isTokenValid(String token, UserDetails userDetails);

    Jwt generateToken(Token token);

}
