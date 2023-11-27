package com.omarahmed42.socialmedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.omarahmed42.socialmedia.dto.request.LoginRequest;
import com.omarahmed42.socialmedia.dto.request.SignupRequest;
import com.omarahmed42.socialmedia.dto.response.JwtResponse;
import com.omarahmed42.socialmedia.enums.Roles;
import com.omarahmed42.socialmedia.exception.EmailAlreadyExistsException;
import com.omarahmed42.socialmedia.exception.UnderAgeException;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.repository.RoleRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class AuthenticationServiceImplTest {

    @Autowired
    private AuthenticationService authenticationService;

    @SpyBean
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @SpyBean
    private PasswordEncoder passwordEncoder;

    @SpyBean
    private UserDetailsService userDetailsService;

    @Value("${test.secret.email0}")
    private String testEmail0;

    @Value("${test.secret.email1}")
    private String testEmail1;

    private User testUser;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeAll
    public void setup() {
        testUser = new User();
        testUser.setFirstName("Setup");
        testUser.setLastName("Test");
        testUser.setEmail(testEmail0);
        testUser.setPassword("$2a$10$m7QAig2lcDrmad9iCR7EZugV0m37JxtSHvV6iBcNqD2nvL6xvl.wi"); // Password: test_pass
        testUser.setDateOfBirth(LocalDate.parse("2000-01-01"));

        testUser.setEnabled(true);
        testUser.setActive(true);

        testUser.addRole(new Role(Roles.USER.getValue()));

        testUser = userRepository.save(testUser);
        reset(userRepository);
    }

    @Test
    void Signup_valid_Should_be_successful() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setFirstName("Random");
        signupRequest.setLastName("Name");
        signupRequest.setPassword("test_pass");
        signupRequest.setEmail(testEmail1);
        signupRequest.setDateOfBirth(LocalDate.parse("2000-01-01"));

        Long response = authenticationService.signUp(signupRequest);

        verify(passwordEncoder).encode(any());
        verify(userRepository).save(any(User.class));
        verify(kafkaTemplate).send(anyString(), anyString(), anyLong());

        assertEquals(Roles.USER.toString(), roleRepository.findById(1).get().getName());
        assertNotNull(response);
        assertEquals(2L, response);
    }

    @Test
    void Signup_Age_Under_16_Should_throw_UnderAgeException() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setFirstName("Someone");
        signupRequest.setLastName("Young");
        signupRequest.setPassword("test_pass");
        signupRequest.setEmail(testEmail1);
        signupRequest.setDateOfBirth(LocalDate.parse("2020-01-01"));

        assertThrows(UnderAgeException.class, () -> authenticationService.signUp(signupRequest));
    }

    @Test
    void Signup_Duplicate_Email_Should_throw_EmailAlreadyExistsException() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setFirstName("Someone");
        signupRequest.setLastName("Twin");
        signupRequest.setPassword("test_pass");
        signupRequest.setEmail(testEmail0);
        signupRequest.setDateOfBirth(LocalDate.parse("2000-01-01"));

        assertThrows(EmailAlreadyExistsException.class, () -> authenticationService.signUp(signupRequest));
        verify(userRepository).existsByEmail(anyString());
    }

    @Test
    void Login_correct_email_and_password_Should_return_a_jwt_sucessfully() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail0);
        loginRequest.setPassword("test_pass");

        JwtResponse response = authenticationService.login(loginRequest);

        verify(passwordEncoder).matches(anyString(), anyString());
        verify(userDetailsService, times(2)).loadUserByUsername(anyString());

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotEquals("", response.getToken());
    }

    @Test
    void Login_correct_email_but_incorrect_password_Should_throw_BadCredentialsException() {
        LoginRequest loginRequest = new LoginRequest(testEmail0, "incorrect_pass");
        assertThrows(BadCredentialsException.class, () -> authenticationService.login(loginRequest));
    }

    @Test
    void Login_incorrect_email_but_correct_password_Should_throw_BadCredentialsException() {
        String randomNotStoredEmail = UUID.randomUUID().toString().replace("-", "") + "@gmail.com";
        LoginRequest loginRequest = new LoginRequest(randomNotStoredEmail, "test_pass");
        assertThrows(BadCredentialsException.class, () -> authenticationService.login(loginRequest));
    }
}
