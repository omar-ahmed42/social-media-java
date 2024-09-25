package com.omarahmed42.socialmedia.service;

import com.omarahmed42.socialmedia.builder.Token;

public interface RefreshTokenService {
    void storeToken(Token token, Long userId);
}
