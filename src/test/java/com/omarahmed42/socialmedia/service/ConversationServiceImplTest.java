package com.omarahmed42.socialmedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.transaction.annotation.Transactional;

import com.omarahmed42.socialmedia.annotation.WithTestUser;
import com.omarahmed42.socialmedia.enums.Roles;
import com.omarahmed42.socialmedia.exception.ForbiddenConversationAccessException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.UserNotFoundException;
import com.omarahmed42.socialmedia.model.Conversation;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.projection.ConversationDetailsProjection;
import com.omarahmed42.socialmedia.repository.ConversationRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@WithTestUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, username = "test.secret.email0")
class ConversationServiceImplTest {
    @Autowired
    private ConversationService conversationService;

    @Value("${test.secret.email0}")
    private String testEmail0;

    @Value("${test.secret.email1}")
    private String testEmail1;

    private User testUser;
    private User testUser2;

    @SpyBean
    private ConversationRepository conversationRepository;

    @SpyBean
    private UserRepository userRepository;

    @MockBean
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
    void addConversation_creator_id_null_should_throw_InvalidInputException() {
        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        Set<Long> membersIds = new HashSet<>();

        assertThrows(InvalidInputException.class, () -> conversationService.addConversation(null, details, membersIds),
                "Conversation creator cannot be null");
    }

    @Test
    void addConversation_details_null_should_throw_InvalidInputException() {
        Long creatorId = testUser.getId();
        ConversationDetailsProjection details = null;
        Set<Long> membersIds = new HashSet<>();

        assertThrows(InvalidInputException.class,
                () -> conversationService.addConversation(creatorId, details, membersIds),
                "Conversation details cannot be null");
    }

    @Test
    void addConversation_isGroup_null_should_throw_InvalidInputException() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(null);

        Set<Long> membersIds = new HashSet<>();

        assertThrows(InvalidInputException.class,
                () -> conversationService.addConversation(creatorId, details, membersIds),
                "Conversation type must be specified");
    }

    @Test
    void addConversation_membersIds_null_should_throw_InvalidInputException() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(true);

        Set<Long> membersIds = null;

        assertThrows(InvalidInputException.class,
                () -> conversationService.addConversation(creatorId, details, membersIds),
                "Conversation members must consist of at least 2 users");
    }

    @Test
    void addConversation_membersIds_size_not_enough_should_throw_InvalidInputException() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(true);

        Set<Long> membersIds = new HashSet<>();

        assertThrows(InvalidInputException.class,
                () -> conversationService.addConversation(creatorId, details, membersIds),
                "Conversation members must consist of at least 2 users");
    }

    @Test
    void addConversation_missing_membersIds_should_throw_UserNotFound() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(true);

        Set<Long> membersIds = new HashSet<>();
        membersIds.add(94L);
        membersIds.add(98L);

        assertThrows(UserNotFoundException.class,
                () -> conversationService.addConversation(creatorId, details, membersIds),
                "Users with ids " + membersIds.toString() + " not found");
    }

    @Test
    void addConversation_createGroupConversation_creator_blocked_should_throw_ForbiddenConversationAccessExceptionn() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(true);

        Set<Long> membersIds = new HashSet<>();
        membersIds.add(testUser2.getId());

        when(blockingService.isBlocked(anyLong(), anyLong())).thenReturn(true);

        assertThrows(ForbiddenConversationAccessException.class,
                () -> conversationService.addConversation(creatorId, details, membersIds),
                "Some user cannot be added to the conversation");
    }

    @Test
    void addConversation_createGroupConversation_should_be_saved_successfully_along_with_members() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(true);

        Set<Long> membersIds = new HashSet<>();
        membersIds.add(testUser2.getId());

        when(blockingService.isBlocked(anyLong(), anyLong())).thenReturn(false);

        Conversation conversation = conversationService.addConversation(creatorId, details, membersIds);

        verify(conversationRepository).save(any(Conversation.class));

        assertNotNull(conversation);
        assertNotNull(conversation.getName());
        assertNotNull(conversation.getConversationMembers());
        assertEquals(2, conversation.getConversationMembers().size());
    }

    @Test
    void addConversation_createNormalConversation_membersIds_null_should_throw_InvalidInputException() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(false);

        Set<Long> membersIds = null;

        assertThrows(InvalidInputException.class,
                () -> conversationService.addConversation(creatorId, details, membersIds),
                "Conversation members must consist of at least 2 users");
    }

    @Test
    void addConversation_createNormalConversation_membersIds_size_not_enough_should_throw_InvalidInputException() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(false);

        Set<Long> membersIds = new HashSet<>();

        assertThrows(InvalidInputException.class,
                () -> conversationService.addConversation(creatorId, details, membersIds),
                "Conversation members must consist of at least 2 users");
    }

    @Test
    void addConversation_createNormalConversation_missing_member_should_throw_InvalidInputException() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(false);

        Set<Long> membersIds = new HashSet<>();
        Long missingMemberId = 945L;
        membersIds.add(missingMemberId);

        assertThrows(UserNotFoundException.class,
                () -> conversationService.addConversation(creatorId, details, membersIds),
                "User with id " + missingMemberId + " not found");
    }

    @Test
    void addConversation_createNormalConversation_creator_blocked_should_throw_ForbiddenConversationAccessExceptionn() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(false);

        Set<Long> membersIds = new HashSet<>();
        membersIds.add(testUser2.getId());

        when(blockingService.isBlocked(anyLong(), anyLong())).thenReturn(true);

        assertThrows(ForbiddenConversationAccessException.class,
                () -> conversationService.addConversation(creatorId, details, membersIds),
                "Some user cannot be added to the conversation");
    }

    @Test
    void addConversation_createNormalConversation_should_be_saved_successfully_along_with_members() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(false);

        Set<Long> membersIds = new HashSet<>();
        membersIds.add(testUser2.getId());

        when(blockingService.isBlocked(anyLong(), anyLong())).thenReturn(false);

        Conversation conversation = conversationService.addConversation(creatorId, details, membersIds);

        verify(conversationRepository).save(any(Conversation.class));

        assertNotNull(conversation);
        assertEquals(details.getName(), conversation.getName());
        assertNotNull(conversation.getConversationMembers());
        assertEquals(2, conversation.getConversationMembers().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void addConversation_createNormalConversation_for_an_existing_conversation_should_return_the_existing_conversation() {
        Long creatorId = testUser.getId();

        ConversationDetailsProjection details = mock(ConversationDetailsProjection.class);
        when(details.isGroup()).thenReturn(false);

        Set<Long> membersIds = new HashSet<>();
        membersIds.add(testUser2.getId());

        Conversation mockedConversation = mock(Conversation.class);
        Optional<Conversation> optionalConversation = mock(Optional.class);

        when(blockingService.isBlocked(anyLong(), anyLong())).thenReturn(false);

        when(conversationRepository.findPersonalConversationBy(anyLong(), anyLong())).thenReturn(optionalConversation);
        when(optionalConversation.isPresent()).thenReturn(true);
        when(optionalConversation.get()).thenReturn(mockedConversation);

        Conversation conversation = conversationService.addConversation(creatorId, details, membersIds);

        verify(conversationRepository, never()).save(any(Conversation.class));

        assertEquals(mockedConversation, conversation);
    }

}
