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
import java.io.InputStream;
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
import com.omarahmed42.socialmedia.enums.CommentStatus;
import com.omarahmed42.socialmedia.enums.PostStatus;
import com.omarahmed42.socialmedia.enums.Roles;
import com.omarahmed42.socialmedia.exception.AuthenticationException;
import com.omarahmed42.socialmedia.exception.CommentNotFoundException;
import com.omarahmed42.socialmedia.exception.ForbiddenCommentAccessException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.UnsupportedMediaExtensionException;
import com.omarahmed42.socialmedia.model.Attachment;
import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.repository.AttachmentRepository;
import com.omarahmed42.socialmedia.repository.CommentAttachmentRepository;
import com.omarahmed42.socialmedia.repository.CommentRepository;
import com.omarahmed42.socialmedia.repository.PostRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@WithTestUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, username = "test.secret.email0")
class CommentAttachmentServiceImplTest {

    @MockBean
    private FileService fileService;

    @Autowired
    private CommentAttachmentService commentAttachmentService;

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
    private CommentRepository commentRepository;

    @SpyBean
    private PostRepository postRepository;

    @SpyBean
    private CommentAttachmentRepository commentAttachmentRepository;

    @SpyBean
    private AttachmentRepository attachmentRepository;

    private Post publishedPost;
    private Post publishedPostByTestUser2;
    private Post archivedPost;

    private Comment commentOnPublishedPost;
    private Comment commentOnPublishedPostByTestUser2;
    private Comment commentOnArchivedPost;

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
        reset(commentRepository);

        commentOnPublishedPost = new Comment();
        commentOnPublishedPost.setPost(publishedPost);
        commentOnPublishedPost.setContent("My comment content on published post");
        commentOnPublishedPost.setCommentStatus(CommentStatus.DRAFT);
        commentOnPublishedPost.setUser(testUser);
        commentOnPublishedPost = commentRepository.save(commentOnPublishedPost);

        commentOnArchivedPost = new Comment();
        commentOnArchivedPost.setPost(archivedPost);
        commentOnArchivedPost.setContent("My comment content on archived post");
        commentOnArchivedPost.setCommentStatus(CommentStatus.DRAFT);
        commentOnArchivedPost.setUser(testUser);
        commentOnArchivedPost = commentRepository.save(commentOnArchivedPost);

        commentOnPublishedPostByTestUser2 = new Comment();
        commentOnPublishedPostByTestUser2.setPost(publishedPost);
        commentOnPublishedPostByTestUser2.setContent("My comment test user2 content on published post");
        commentOnPublishedPostByTestUser2.setCommentStatus(CommentStatus.DRAFT);
        commentOnPublishedPostByTestUser2.setUser(testUser2);
        commentOnPublishedPostByTestUser2 = commentRepository.save(commentOnPublishedPostByTestUser2);

    }

    @Test
    @WithAnonymousUser
    void saveCommentAttachment_not_authenticated_should_throw_AuthenticationException() throws IOException {
        Long commentId = commentOnPublishedPost.getId();
        MultipartFile mockedMultipartFile = mock(MultipartFile.class);

        assertThrows(AuthenticationException.class,
                () -> commentAttachmentService.saveCommentAttachment(mockedMultipartFile, commentId));
    }

    @Test
    void saveCommentAttachment_invalid_attachment_extension() {
        Long commentId = commentOnPublishedPost.getId();

        MultipartFile mockedMultipartFile = mock(MultipartFile.class);
        when(mockedMultipartFile.getOriginalFilename()).thenReturn("my_file.ext");

        assertThrows(UnsupportedMediaExtensionException.class,
                () -> commentAttachmentService.saveCommentAttachment(mockedMultipartFile, commentId),
                "ext extension is not supported");
    }

    @Test
    void saveCommentAttachment_missing_post_should_throw_CommentNotFoundException() {
        MultipartFile mockedMultipartFile = mock(MultipartFile.class);
        when(mockedMultipartFile.getOriginalFilename()).thenReturn("my_file.jpg");

        assertThrows(CommentNotFoundException.class,
                () -> commentAttachmentService.saveCommentAttachment(mockedMultipartFile, 5959L));
    }

    @Test
    void saveCommentAttachment_not_comment_owner_should_throw_ForbiddenCommentAccessException() {
        Long commentId = commentOnPublishedPostByTestUser2.getId();

        MultipartFile mockedMultipartFile = mock(MultipartFile.class);
        when(mockedMultipartFile.getOriginalFilename()).thenReturn("my_file.jpg");

        assertThrows(ForbiddenCommentAccessException.class,
                () -> commentAttachmentService.saveCommentAttachment(mockedMultipartFile, commentId),
                "Forbidden: Cannot access comment with id " + commentId);
    }

    @Test
    void saveCommentAttachment_comment_on_archived_post_should_throw_InvalidInputException() {
        Long commentId = commentOnArchivedPost.getId();

        MultipartFile mockedMultipartFile = mock(MultipartFile.class);
        when(mockedMultipartFile.getOriginalFilename()).thenReturn("my_file.jpg");

        assertThrows(InvalidInputException.class,
                () -> commentAttachmentService.saveCommentAttachment(mockedMultipartFile, commentId),
                "Cannot comment on a non-published post");
    }

    @Test
    void saveCommentAttachment_should_save_attachment_store_file_successfully() throws IOException {
        Long commentId = commentOnPublishedPost.getId();

       MockMultipartFile mockedMultipartFile = new MockMultipartFile("my_name", "my_file.jpg", null,
                "file".getBytes());

        when(fileService.copy(any(InputStream.class), any(Path.class))).thenReturn(0L);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        Long attachmentId = commentAttachmentService.saveCommentAttachment(mockedMultipartFile, commentId);

        verify(kafkaTemplate).send(anyString(), anyString(), any());
        verify(attachmentRepository).save(any(Attachment.class));

        assertNotNull(attachmentId);
    }
}