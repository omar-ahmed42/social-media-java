package com.omarahmed42.socialmedia.service;

import com.omarahmed42.socialmedia.dto.request.LoginRequest;
import com.omarahmed42.socialmedia.dto.request.SignupRequest;
import com.omarahmed42.socialmedia.dto.response.JwtResponse;

public interface AuthenticationService {
    Long signUp(SignupRequest request);
    JwtResponse login(LoginRequest request);
}
