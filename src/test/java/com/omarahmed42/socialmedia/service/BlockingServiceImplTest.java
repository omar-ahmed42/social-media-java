package com.omarahmed42.socialmedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
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
import com.omarahmed42.socialmedia.exception.AuthenticationException;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.model.graph.UserNode;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.repository.graph.UserNodeRepository;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@WithTestUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, username = "test.secret.email0")
class BlockingServiceImplTest {

    private UserNode blocker;
    private UserNode blocked;

    @Value("${test.secret.email0}")
    private String testEmail0;

    @SpyBean
    private UserNodeRepository userNodeRepository;

    @SpyBean
    private UserRepository userRepository;

    @Autowired
    private BlockingService blockingService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeAll
    void setup() {
        User testUser = new User();
        testUser.setFirstName("Setup");
        testUser.setLastName("Test");
        testUser.setEmail(testEmail0);
        testUser.setPassword("$2a$10$m7QAig2lcDrmad9iCR7EZugV0m37JxtSHvV6iBcNqD2nvL6xvl.wi"); // Password: test_pass
        testUser.setDateOfBirth(LocalDate.parse("2000-01-01"));
        testUser = userRepository.save(testUser);

        blocker = new UserNode(1L);
        blocked = new UserNode(2L);
        blocker = userNodeRepository.save(blocker);
        blocked = userNodeRepository.save(blocked);
        reset(userNodeRepository);
    }

    @AfterEach
    void tearDown() {
        blocker.getBlockedUsers().clear();
        blocker = userNodeRepository.save(blocker);
        reset(userNodeRepository);
    }

    @Test
    @WithAnonymousUser
    void blockUser_not_authenticated_should_throw_AuthenticationException() {
        Long blockedUserId = blocked.getUserId();
        assertThrows(AuthenticationException.class, () -> blockingService.blockUser(blockedUserId));
    }

    @Test
    void blockUser_should_work_successfully_and_return_true() {
        boolean result = blockingService.blockUser(blocked.getUserId());

        when(kafkaTemplate.send(anyString(), anyString(), any(Object.class))).thenReturn(null);
        verify(userNodeRepository).save(any(UserNode.class));
        UserNode actual = userNodeRepository.findById(blocker.getUserId()).orElse(null);

        assertNotNull(actual);
        assertEquals(1, actual.getBlockedUsers().size());
        Long id = actual.getBlockedUsers()
                .stream()
                .map(UserNode::getUserId)
                .filter(u -> u.equals(blocked.getUserId()))
                .findFirst().orElse(null);

        assertNotNull(id);
        assertEquals(blocked.getUserId(), id);
        assertTrue(result);
    }

    @Test
    @WithAnonymousUser
    void unblockUser_not_authenticated_should_throw_AuthenticationException() {
        Long blockedUserId = blocked.getUserId();
        assertThrows(AuthenticationException.class, () -> blockingService.unblockUser(blockedUserId));
    }

    @Test
    void unblockUser_should_work_successfully_and_return_true() throws InterruptedException {
        blocker.getBlockedUsers().add(blocked);
        blocker = userNodeRepository.save(blocker);
        reset(userNodeRepository);

        boolean result = blockingService.unblockUser(blocked.getUserId());

        verify(userNodeRepository).deleteBlocksRelationshipBetween(anyLong(), anyLong());
        UserNode actual = userNodeRepository.findById(blocker.getUserId()).orElse(null);

        assertTrue(result);
        assertNotNull(actual);
        assertEquals(0, actual.getBlockedUsers().size());
    }

}
