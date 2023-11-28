package com.omarahmed42.socialmedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.omarahmed42.socialmedia.annotation.WithTestUser;
import com.omarahmed42.socialmedia.enums.MessageStatus;
import com.omarahmed42.socialmedia.enums.Roles;
import com.omarahmed42.socialmedia.exception.AuthenticationException;
import com.omarahmed42.socialmedia.exception.ConversationNotFoundException;
import com.omarahmed42.socialmedia.exception.ForbiddenConversationAccessException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.model.Conversation;
import com.omarahmed42.socialmedia.model.ConversationMember;
import com.omarahmed42.socialmedia.model.Message;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.repository.ConversationRepository;
import com.omarahmed42.socialmedia.repository.MessageRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@WithTestUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, username = "test.secret.email0")
class MessageServiceImplTest {

    @Autowired
    private MessageService messageService;

    @MockBean
    private FileService fileService;

    @SpyBean
    private MessageRepository messageRepository;

    @MockBean
    private ConversationRepository conversationRepository;

    @MockBean
    private ConversationService conversationService;

    @Value("${test.secret.email0}")
    private String testEmail0;

    @Value("${test.secret.email1}")
    private String testEmail1;

    private User testUser;
    private User testUser2;

    @SpyBean
    private UserRepository userRepository;

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
    void addPersonalMessage_unauthenticated_should_throw_AuthenticationException() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        Long receiverId = testUser2.getId();

        assertThrows(AuthenticationException.class,
                () -> messageService.addPersonalMessage(multipartFile, receiverId, "My message content"));
    }

    @ParameterizedTest(name = "content")
    @ValueSource(strings = { "", "   " })
    @NullSource
    void addPersonalMessage_empty_content_and_file_should_throw_InvalidInputException(String content) {
        MultipartFile multipartFile = null;
        Long receiverId = testUser2.getId();

        Conversation conversation = new Conversation();
        conversation.setId(1L);
        Optional<Conversation> optionalConversation = Optional.of(conversation);
        when(conversationRepository.findPersonalConversationBy(anyLong(), anyLong())).thenReturn(optionalConversation);

        assertThrows(InvalidInputException.class,
                () -> messageService.addPersonalMessage(multipartFile, receiverId, content),
                "Message cannot be empty");
    }

    @Test
    void addPersonalMessage_no_file_should_add_message_without_attachment() {
        MultipartFile multipartFile = null;
        Long receiverId = testUser2.getId();

        Conversation conversation = new Conversation();
        conversation.setId(1L);
        Optional<Conversation> optionalConversation = Optional.of(conversation);
        when(conversationRepository.findPersonalConversationBy(anyLong(), anyLong())).thenReturn(optionalConversation);

        Message message = messageService.addPersonalMessage(multipartFile, receiverId, "My message content");

        verify(messageRepository).save(any(Message.class));

        assertNotNull(message);
        assertNotNull(message.getId());
        assertNull(message.getAttachmentUrl());
        assertEquals(1L, message.getConversationId());
        assertEquals(testUser.getId(), message.getUserId());
        assertEquals(MessageStatus.SENT, message.getMessageStatus());
    }

    @Test
    void addPersonalMessage_attachment_and_content_included_should_be_added_successfully() throws IOException {
        MockMultipartFile mockedMultipartFile = new MockMultipartFile("my_name", "my_file.jpg", null,
                "file".getBytes());

        Long receiverId = testUser2.getId();

        Conversation conversation = new Conversation();
        conversation.setId(1L);
        Optional<Conversation> optionalConversation = Optional.of(conversation);
        when(conversationRepository.findPersonalConversationBy(anyLong(), anyLong())).thenReturn(optionalConversation);

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class)) {
            FileUtils.forceMkdir(any(File.class));
        }

        when(fileService.copy(any(InputStream.class), any(Path.class))).thenReturn(100L);

        Message message = messageService.addPersonalMessage(mockedMultipartFile, receiverId, "My message content");

        verify(messageRepository).save(any(Message.class));

        assertNotNull(message);
        assertNotNull(message.getId());
        assertNotNull(message.getAttachmentUrl());
        assertEquals(1L, message.getConversationId());
        assertEquals(testUser.getId(), message.getUserId());
        assertEquals(MessageStatus.SENT, message.getMessageStatus());
    }

    // ------------

    @Test
    @WithAnonymousUser
    void addMessage_unauthenticated_should_throw_AuthenticationException() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        Long conversationId = 1L;

        assertThrows(AuthenticationException.class,
                () -> messageService.addMessage(multipartFile, conversationId, "My message content"));
    }

    @Test
    void addMessage_missing_conversation_should_throw_ConversationNotFound() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        Long conversationId = 565656L;

        assertThrows(ConversationNotFoundException.class,
                () -> messageService.addMessage(multipartFile, conversationId, "My message content"));
    }

    @Test
    void addMessage_empty_conversationId_should_throw_InvalidInputException() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        Long conversationId = null;

        assertThrows(InvalidInputException.class,
                () -> messageService.addMessage(multipartFile, conversationId, "My message content"),
                "Conversation id cannot be null");
    }

    @Test
    void addMessage_authenticated_user_not_a_member_of_the_conversation_should_throw_ForbiddenConversationAccessException() {
        MultipartFile multipartFile = null;
        Long conversationId = 1L;

        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.getConversationMembers().add(new ConversationMember(2L, conversation, testUser2));

        Optional<Conversation> optionalConversation = Optional.of(conversation);
        when(conversationRepository.findConversationById(anyLong())).thenReturn(optionalConversation);

        assertThrows(ForbiddenConversationAccessException.class,
                () -> messageService.addMessage(multipartFile, conversationId, "My message content"),
                "Forbidden: cannot send a message to conversation with id " + conversationId);
    }

    @ParameterizedTest(name = "content")
    @ValueSource(strings = { "", "   " })
    @NullSource
    void addMessage_empty_content_and_file_should_throw_InvalidInputException(String content) {
        MultipartFile multipartFile = null;
        Long conversationId = 1L;

        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.getConversationMembers().add(new ConversationMember(1L, conversation, testUser));
        conversation.getConversationMembers().add(new ConversationMember(2L, conversation, testUser2));

        Optional<Conversation> optionalConversation = Optional.of(conversation);
        when(conversationRepository.findConversationById(anyLong())).thenReturn(optionalConversation);
        when(conversationRepository.findConversationById(anyLong())).thenReturn(optionalConversation);

        assertThrows(InvalidInputException.class,
                () -> messageService.addMessage(multipartFile, conversationId, content),
                "Message cannot be empty");
    }

    @Test
    void addMessage_no_file_should_add_message_without_attachment() {
        MultipartFile multipartFile = null;
        Long conversationId = 1L;

        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.getConversationMembers().add(new ConversationMember(1L, conversation, testUser));
        conversation.getConversationMembers().add(new ConversationMember(2L, conversation, testUser2));

        Optional<Conversation> optionalConversation = Optional.of(conversation);
        when(conversationRepository.findConversationById(anyLong())).thenReturn(optionalConversation);
        when(conversationRepository.findPersonalConversationBy(anyLong(), anyLong())).thenReturn(optionalConversation);

        Message message = messageService.addMessage(multipartFile, conversationId, "My message content");

        verify(messageRepository).save(any(Message.class));

        assertNotNull(message);
        assertNotNull(message.getId());
        assertNull(message.getAttachmentUrl());
        assertEquals(1L, message.getConversationId());
        assertEquals(testUser.getId(), message.getUserId());
        assertEquals(MessageStatus.SENT, message.getMessageStatus());
    }

    @Test
    void addMessage_attachment_and_content_included_should_be_added_successfully() throws IOException {
        MockMultipartFile multipartFile = new MockMultipartFile("my_name", "my_file.jpg", null,
                "file".getBytes());

        Long conversationId = 1L;

        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.getConversationMembers().add(new ConversationMember(1L, conversation, testUser));
        conversation.getConversationMembers().add(new ConversationMember(2L, conversation, testUser2));

        Optional<Conversation> optionalConversation = Optional.of(conversation);
        when(conversationRepository.findConversationById(anyLong())).thenReturn(optionalConversation);
        when(conversationRepository.findPersonalConversationBy(anyLong(), anyLong())).thenReturn(optionalConversation);

        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class)) {
            FileUtils.forceMkdir(any(File.class));
        }

        when(fileService.copy(any(InputStream.class), any(Path.class))).thenReturn(100L);

        Message message = messageService.addMessage(multipartFile, conversationId, "My message content");

        verify(messageRepository).save(any(Message.class));

        assertNotNull(message);
        assertNotNull(message.getId());
        assertNotNull(message.getAttachmentUrl());
        assertEquals(1L, message.getConversationId());
        assertEquals(testUser.getId(), message.getUserId());
        assertEquals(MessageStatus.SENT, message.getMessageStatus());
    }
}
