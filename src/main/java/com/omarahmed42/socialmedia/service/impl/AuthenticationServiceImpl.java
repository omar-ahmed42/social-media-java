package com.omarahmed42.socialmedia.service.impl;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omarahmed42.socialmedia.dto.request.LoginRequest;
import com.omarahmed42.socialmedia.dto.request.SignupRequest;
import com.omarahmed42.socialmedia.dto.response.JwtResponse;
import com.omarahmed42.socialmedia.enums.Roles;
import com.omarahmed42.socialmedia.exception.EmailAlreadyExistsException;
import com.omarahmed42.socialmedia.exception.UnderAgeException;
import com.omarahmed42.socialmedia.mapper.UserMapper;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.repository.RoleRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.service.AuthenticationService;
import com.omarahmed42.socialmedia.service.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest request) {
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new JwtResponse(token);
    }

    @Override
    @Transactional
    public Long signUp(SignupRequest request) {
        User user = userMapper.toEntity(request);

        if (!isValidAge(request.getDateOfBirth()))
            throw new UnderAgeException("User must be atleast " + getMinimumAge() + " years old");

        if (userRepository.existsByEmail(request.getEmail()))
            throw new EmailAlreadyExistsException();
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);

        user.getRoles().add(roleRepository.getReferenceById(Roles.USER.getValue()));
        user = userRepository.save(user);
        return user.getId();
    }

    private boolean isValidAge(LocalDate dateOfBirth) {
        int actualAge = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return actualAge >= getMinimumAge();
    }

    private int getMinimumAge() {
        return 16;
    }

}
