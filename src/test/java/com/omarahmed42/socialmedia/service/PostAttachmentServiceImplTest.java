package com.omarahmed42.socialmedia.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.omarahmed42.socialmedia.annotation.WithTestUser;
import com.omarahmed42.socialmedia.enums.PostStatus;
import com.omarahmed42.socialmedia.enums.Roles;
import com.omarahmed42.socialmedia.exception.AuthenticationException;
import com.omarahmed42.socialmedia.exception.ForbiddenPostAccessException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.PostNotFoundException;
import com.omarahmed42.socialmedia.exception.UnsupportedMediaExtensionException;
import com.omarahmed42.socialmedia.model.Attachment;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.repository.AttachmentRepository;
import com.omarahmed42.socialmedia.repository.PostAttachmentRepository;
import com.omarahmed42.socialmedia.repository.PostRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@WithTestUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, username = "test.secret.email0")
class PostAttachmentServiceImplTest {

    @Autowired
    private PostAttachmentService postAttachmentService;

    @Value("${test.secret.email0}")
    private String testEmail0;

    @Value("${test.secret.email1}")
    private String testEmail1;

    private User testUser;
    private User testUser2;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @SpyBean
    private UserRepository userRepository;

    @SpyBean
    private PostRepository postRepository;

    @SpyBean
    private PostAttachmentRepository postAttachmentRepository;

    @SpyBean
    private AttachmentRepository attachmentRepository;

    @MockBean
    private FileService fileService;

    private Post publishedPost;
    private Post publishedPostByTestUser2;
    private Post archivedPost;

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

        publishedPost = new Post();
        publishedPost.setUser(testUser);
        publishedPost.setContent("My content");
        publishedPost.setPostStatus(PostStatus.PUBLISHED);
        publishedPost = postRepository.save(publishedPost);

        archivedPost = new Post();
        archivedPost.setUser(testUser);
        archivedPost.setContent("My content");
        archivedPost.setPostStatus(PostStatus.ARCHIVED);
        archivedPost = postRepository.save(archivedPost);

        publishedPostByTestUser2 = new Post();
        publishedPostByTestUser2.setUser(testUser2);
        publishedPostByTestUser2.setContent("My content");
        publishedPostByTestUser2.setPostStatus(PostStatus.PUBLISHED);
        publishedPostByTestUser2 = postRepository.save(publishedPostByTestUser2);
        reset(postRepository);
    }

    @Test
    @WithAnonymousUser
    void savePostAttachment_not_authenticated_should_throw_AuthenticationException() throws IOException {
        Long postId = publishedPost.getId();
        MultipartFile mockedMultipartFile = mock(MultipartFile.class);

        assertThrows(AuthenticationException.class,
                () -> postAttachmentService.savePostAttachment(mockedMultipartFile, postId));
    }

    @Test
    void savePostAttachment_invalid_attachment_extension() {
        Long postId = publishedPost.getId();

        MultipartFile mockedMultipartFile = mock(MultipartFile.class);
        when(mockedMultipartFile.getOriginalFilename()).thenReturn("my_file.ext");

        assertThrows(UnsupportedMediaExtensionException.class,
                () -> postAttachmentService.savePostAttachment(mockedMultipartFile, postId),
                "ext extension is not supported");
    }

    @Test
    void savePostAttachment_missing_post_should_throw_PostNotFoundException() {
        MockMultipartFile mockedMultipartFile = new MockMultipartFile("my_name", "my_file.jpg", null,
                "file".getBytes());

        assertThrows(PostNotFoundException.class,
                () -> postAttachmentService.savePostAttachment(mockedMultipartFile, 5959L));
    }

    @Test
    void savePostAttachment_not_post_owner_should_throw_ForbiddenPostAccessException() {
        Long postId = publishedPostByTestUser2.getId();

        MockMultipartFile mockedMultipartFile = new MockMultipartFile("my_name", "my_file.jpg", null,
                "file".getBytes());

        assertThrows(ForbiddenPostAccessException.class,
                () -> postAttachmentService.savePostAttachment(mockedMultipartFile, postId),
                "Forbidden: Cannot access post with id " + postId);
    }

    @Test
    void savePostAttachment_archived_post_should_throw_InvalidInputException() {
        Long postId = archivedPost.getId();

        MultipartFile mockedMultipartFile = mock(MultipartFile.class);
        when(mockedMultipartFile.getOriginalFilename()).thenReturn("my_file.jpg");

        assertThrows(InvalidInputException.class,
                () -> postAttachmentService.savePostAttachment(mockedMultipartFile, postId),
                "Cannot update an archived post");
    }

    @Test
    void savePostAttachment_should_save_attachment_store_file_successfully() throws IOException {
        Long postId = publishedPost.getId();

        MockMultipartFile mockedMultipartFile = new MockMultipartFile("my_name", "my_file.jpg", null,
                "file".getBytes());
        when(fileService.copy(mockedMultipartFile.getInputStream(), Path.of("D:\\fa\\uploads"))).thenReturn(100L);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        Long attachmentId = postAttachmentService.savePostAttachment(mockedMultipartFile, postId);

        verify(kafkaTemplate).send(anyString(), anyString(), any());
        verify(attachmentRepository).save(any(Attachment.class));

        assertNotNull(attachmentId);
    }
}
