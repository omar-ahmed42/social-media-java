package com.omarahmed42.socialmedia.service.impl;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
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
import com.omarahmed42.socialmedia.exception.UsernameAlreadyExistsException;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int MINIMUM_AGE = 16;

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

        throwIfInvalidAge(request.getDateOfBirth());
        throwIfUsernameExists(request.getUsername());
        throwIfEmailExists(request.getEmail());

        hashAndSetPassword(user, user.getPassword());

        user.getRoles().add(roleRepository.getReferenceById(Roles.USER.getValue()));
        user = userRepository.save(user);

        kafkaTemplate.send("graph-user-creation", UUID.randomUUID().toString(), user.getId());
        return user.getId();
    }

    private void throwIfInvalidAge(LocalDate dateOfBirth) {
        if (!isValidAge(dateOfBirth))
            throw new UnderAgeException("User must be atleast " + getMinimumAge() + " years old");
    }

    private boolean isValidAge(LocalDate dateOfBirth) {
        int actualAge = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return actualAge >= getMinimumAge();
    }

    private int getMinimumAge() {
        return MINIMUM_AGE;
    }

    private void throwIfUsernameExists(String username) {
        if (userRepository.existsByUsername(username))
            throw new UsernameAlreadyExistsException();
    }

    private void throwIfEmailExists(String email) {
        if (userRepository.existsByEmail(email))
            throw new EmailAlreadyExistsException();
    }

    private void hashAndSetPassword(User user, String plainPassword) {
        String hashedPassword = passwordEncoder.encode(plainPassword);
        user.setPassword(hashedPassword);
    }
}
