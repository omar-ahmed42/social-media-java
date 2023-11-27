package com.omarahmed42.socialmedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

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
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.transaction.annotation.Transactional;

import com.omarahmed42.socialmedia.annotation.WithTestUser;
import com.omarahmed42.socialmedia.enums.FriendRequestStatus;
import com.omarahmed42.socialmedia.enums.Roles;
import com.omarahmed42.socialmedia.exception.AuthenticationException;
import com.omarahmed42.socialmedia.exception.ForbiddenFriendRequestAccessException;
import com.omarahmed42.socialmedia.exception.FriendRequestNotFoundException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.model.FriendRequest;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.repository.FriendRequestRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@WithTestUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, username = "test.secret.email0")
class FriendRequestServiceImplTest {

    @Autowired
    private FriendRequestService friendRequestService;

    @SpyBean
    private FriendRequestRepository friendRequestRepository;

    @SpyBean
    private UserRepository userRepository;

    @Value("${test.secret.email0}")
    private String testEmail0;

    @Value("${test.secret.email1}")
    private String testEmail1;

    private User testUser;
    private User testUser2;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @SpyBean
    private FriendService friendService;

    @SpyBean
    private BlockingService blockingService;

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

        testUser2 = new User();
        testUser2.setFirstName("Setup2");
        testUser2.setLastName("Test2");
        testUser2.setEmail(testEmail1);
        testUser2.setPassword("$2a$10$m7QAig2lcDrmad9iCR7EZugV0m37JxtSHvV6iBcNqD2nvL6xvl.wi"); // Password: test_pass
        testUser2.setDateOfBirth(LocalDate.parse("2000-01-01"));

        testUser2.setEnabled(true);
        testUser2.setActive(true);

        testUser2.addRole(new Role(Roles.USER.getValue()));

        testUser2 = userRepository.save(testUser2);

        reset(userRepository);
    }

    @Test
    @WithAnonymousUser
    void acceptFriendRequest_AnonymousUser_should_throw_AuthenticationException() {
        assertThrows(AuthenticationException.class,
                () -> friendRequestService.acceptFriendRequest(1L));
    }

    @Test
    void acceptFriendRequest_missing_friend_request_should_throw_FriendRequestNotFoundException() {
        assertThrows(FriendRequestNotFoundException.class, () -> friendRequestService.acceptFriendRequest(515412L));
    }

    private FriendRequest createFriendRequest(User sender, User receiver, FriendRequestStatus status) {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setRequestStatus(status);
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequestRepository.save(friendRequest);
        friendRequest = friendRequestRepository.save(friendRequest);
        reset(friendRequestRepository);
        return friendRequest;
    }

    @Test
    void acceptFriendRequest_not_receiver_should_throw_ForbiddenFriendRequestAccessException() {

        FriendRequest friendRequest = createFriendRequest(testUser, testUser2, FriendRequestStatus.PENDING);

        final Long id = friendRequest.getId();
        assertThrows(ForbiddenFriendRequestAccessException.class,
                () -> friendRequestService.acceptFriendRequest(id),
                "Forbidden: cannot " + "accept" + " friend request with id " + id);
    }

    @Test
    void acceptFriendRequest_status_pending_should_update_status_to_accepted_successfully() {
        FriendRequest friendRequest = createFriendRequest(testUser2, testUser, FriendRequestStatus.PENDING);

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);
        boolean result = friendRequestService.acceptFriendRequest(friendRequest.getId());

        verify(friendRequestRepository).save(any(FriendRequest.class));
        verify(kafkaTemplate).send(anyString(), anyString(), any());

        assertTrue(result);
        FriendRequest actual = friendRequestRepository.findById(friendRequest.getId()).orElse(null);
        assertNotNull(actual);
        assertEquals(FriendRequestStatus.ACCEPTED, actual.getRequestStatus());
    }

    @Test
    void acceptFriendRequest_status_accepted_should_return_false_successfully() {
        FriendRequest friendRequest = createFriendRequest(testUser2, testUser, FriendRequestStatus.ACCEPTED);

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);
        boolean result = friendRequestService.acceptFriendRequest(friendRequest.getId());

        verify(friendRequestRepository, never()).save(any(FriendRequest.class));

        assertTrue(result);
        FriendRequest actual = friendRequestRepository.findById(friendRequest.getId()).orElse(null);
        assertNotNull(actual);
        assertEquals(FriendRequestStatus.ACCEPTED, actual.getRequestStatus());
    }

    @Test
    @WithAnonymousUser
    void rejectFriendRequest_AnonymousUser_should_throw_AuthenticationException() {
        assertThrows(AuthenticationException.class,
                () -> friendRequestService.rejectFriendRequest(1L));
    }

    @Test
    void rejectFriendRequest_missing_friend_request_should_throw_FriendRequestNotFoundException() {
        assertThrows(FriendRequestNotFoundException.class, () -> friendRequestService.rejectFriendRequest(515412L));
    }

    @Test
    void rejectFriendRequest_not_receiver_should_throw_ForbiddenFriendRequestAccessException() {

        FriendRequest friendRequest = createFriendRequest(testUser, testUser2, FriendRequestStatus.PENDING);

        final Long id = friendRequest.getId();
        assertThrows(ForbiddenFriendRequestAccessException.class,
                () -> friendRequestService.rejectFriendRequest(id),
                "Forbidden: cannot " + "reject" + " friend request with id " + id);
    }

    @Test
    void rejectFriendRequest_status_pending_should_update_status_to_rejected_successfully() {
        FriendRequest friendRequest = createFriendRequest(testUser2, testUser, FriendRequestStatus.PENDING);

        boolean result = friendRequestService.rejectFriendRequest(friendRequest.getId());

        verify(friendRequestRepository).save(any(FriendRequest.class));

        assertTrue(result);
        FriendRequest actual = friendRequestRepository.findById(friendRequest.getId()).orElse(null);
        assertNotNull(actual);
        assertEquals(FriendRequestStatus.REJECTED, actual.getRequestStatus());
    }

    @Test
    void rejectFriendRequest_status_rejected_should_return_false_successfully() {
        FriendRequest friendRequest = createFriendRequest(testUser2, testUser, FriendRequestStatus.ACCEPTED);

        boolean result = friendRequestService.rejectFriendRequest(friendRequest.getId());
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));

        assertFalse(result);
        FriendRequest actual = friendRequestRepository.findById(friendRequest.getId()).orElse(null);
        assertNotNull(actual);
        assertEquals(FriendRequestStatus.ACCEPTED, actual.getRequestStatus());
    }

    @Test
    void sendFriendRequest_sender_and_receiver_are_the_same_should_throw_InvalidInputException() {
        Long receiverId = testUser.getId();
        assertThrows(InvalidInputException.class, () -> friendRequestService.sendFriendRequest(receiverId),
                "Sender and Receiver cannot be the same");
    }

    @Test
    @WithAnonymousUser
    void sendFriendRequest_AnonymousUser_should_throw_AuthenticationException() {
        Long receiverId = testUser.getId();
        assertThrows(AuthenticationException.class,
                () -> friendRequestService.sendFriendRequest(receiverId));
    }

    @Test
    void sendFriendRequest_users_are_already_friends_should_not_send_friend_request_and_should_return_null() {
        Long receiverId = testUser2.getId();

        when(friendService.isFriend(anyLong(), anyLong())).thenReturn(true);

        FriendRequest friendRequest = friendRequestService.sendFriendRequest(receiverId);
        verify(friendRequestRepository, never()).save(any());
        assertNull(friendRequest);
    }

    @Test
    void sendFriendRequest_user_is_blocked_should_not_send_friend_request_and_should_return_null() {
        Long receiverId = testUser2.getId();
        when(blockingService.isBlocked(anyLong(), anyLong())).thenReturn(true);

        FriendRequest friendRequest = friendRequestService.sendFriendRequest(receiverId);
        verify(friendRequestRepository, never()).save(any());
        assertNull(friendRequest);
    }

    @Test
    void sendFriendRequest_should_send_friend_request_successfully() {
        Long receiverId = testUser2.getId();
        FriendRequest friendRequest = friendRequestService.sendFriendRequest(receiverId);

        when(friendService.isFriend(anyLong(), anyLong())).thenReturn(false);
        when(blockingService.isBlocked(anyLong(), anyLong())).thenReturn(false);

        assertNotNull(friendRequest);
        assertEquals(FriendRequestStatus.PENDING, friendRequest.getRequestStatus());
    }

    @Test
    @WithAnonymousUser
    void cancelFriendRequest_AnonymousUser_should_throw_AuthenticationException() {
        Long receiverId = testUser.getId();
        assertThrows(AuthenticationException.class,
                () -> friendRequestService.cancelFriendRequest(receiverId));
    }

    @Test
    void cancelFriendRequest_missing_friend_request_should_throw_FriendRequestNotFoundException() {
        assertThrows(FriendRequestNotFoundException.class, () -> friendRequestService.cancelFriendRequest(515412L));
    }

    @Test
    void cancelFriendRequest_not_sender_should_throw_ForbiddenFriendRequestAccessException() {
        FriendRequest friendRequest = createFriendRequest(testUser2, testUser, FriendRequestStatus.PENDING);

        final Long id = friendRequest.getId();
        assertThrows(ForbiddenFriendRequestAccessException.class,
                () -> friendRequestService.cancelFriendRequest(id),
                "Forbidden: cannot " + "cancel" + " friend request with id " + id);
    }

    @Test
    void cancelFriendRequest_should_cancel_friend_request_successfully() {
        FriendRequest friendRequest = createFriendRequest(testUser, testUser2, FriendRequestStatus.PENDING);

        boolean actual = friendRequestService.cancelFriendRequest(friendRequest.getId());

        verify(friendRequestRepository).save(any(FriendRequest.class));
        assertTrue(actual);
        assertNotNull(friendRequest);
    }

    @Test
    void cancelFriendRequest_status_cancelled_should_not_cancel_friend_request_and_should_return_true() {
        FriendRequest friendRequest = createFriendRequest(testUser, testUser2, FriendRequestStatus.CANCELLED);

        boolean actual = friendRequestService.cancelFriendRequest(friendRequest.getId());
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
        assertTrue(actual);
        assertNotNull(friendRequest);
    }

}