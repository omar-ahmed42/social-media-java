package com.omarahmed42.socialmedia.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import com.omarahmed42.socialmedia.configuration.security.CustomUserDetails;
import com.omarahmed42.socialmedia.enums.AttachmentStatus;
import com.omarahmed42.socialmedia.enums.AttachmentType;
import com.omarahmed42.socialmedia.enums.PostStatus;
import com.omarahmed42.socialmedia.enums.Roles;
import com.omarahmed42.socialmedia.exception.AuthenticationException;
import com.omarahmed42.socialmedia.exception.ForbiddenPostAccessException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.PostNotFoundException;
import com.omarahmed42.socialmedia.model.Attachment;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.PostAttachment;
import com.omarahmed42.socialmedia.model.PostAttachmentId;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.projection.PostInputProjection;
import com.omarahmed42.socialmedia.repository.AttachmentRepository;
import com.omarahmed42.socialmedia.repository.PostAttachmentRepository;
import com.omarahmed42.socialmedia.repository.PostRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;
import com.omarahmed42.socialmedia.util.SecurityUtils;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@WithTestUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, username = "test.secret.email0")
class PostServiceImplTest {

    private User testUser;
    private User testUser1;

    @SpyBean
    private UserRepository userRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${test.secret.email0}")
    private String testEmail0;

    @Value("${test.secret.email1}")
    private String testEmail1;

    @SpyBean
    private PostRepository postRepository;

    @Autowired
    private PostService postService;

    @MockBean
    private PostInputProjection postInputProjection;

    @SpyBean
    private PostAttachmentRepository postAttachmentRepository;

    @SpyBean
    private AttachmentRepository attachmentRepository;

    @Value("${storage.path}")
    private String storagePath;

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

        testUser1 = new User();
        testUser1.setFirstName("Setup");
        testUser1.setLastName("Test");
        testUser1.setEmail(testEmail1);
        testUser1.setPassword("$2a$10$m7QAig2lcDrmad9iCR7EZugV0m37JxtSHvV6iBcNqD2nvL6xvl.wi"); // Password: test_pass
        testUser1.setDateOfBirth(LocalDate.parse("2000-01-01"));

        testUser1.setEnabled(true);
        testUser1.setActive(true);

        testUser1.addRole(new Role(Roles.USER.getValue()));

        testUser1 = userRepository.save(testUser1);
        reset(userRepository);
    }

    @BeforeEach
    public void cleanUpAfterTests() {
        postRepository.deleteAll();
        reset(postRepository);
    }

    @Test
    @WithAnonymousUser
    void addPost_using_unauthenticated_user_SHOULD_throw_AuthenticationException() {
        when(postInputProjection.getId()).thenReturn(null);
        when(postInputProjection.getContent()).thenReturn("This is content");
        when(postInputProjection.getPostStatus()).thenReturn("DRAFT");

        assertThrows(AuthenticationException.class, () -> postService.addPost(postInputProjection));
    }

    @Test
    @Tag("addPost")
    @DisplayName("Add a new post with draft status and non-blank content")
    void addPost_should_add_a_new_draft_post_with_content_successfully() {
        when(postInputProjection.getId()).thenReturn(null);
        when(postInputProjection.getContent()).thenReturn("This is content");
        when(postInputProjection.getPostStatus()).thenReturn("DRAFT");

        Post post = postService.addPost(postInputProjection);

        verify(postRepository).save(any(Post.class));

        assertNotNull(post);
        assertNotNull(post.getId());
        assertEquals(1L, post.getId());
        assertEquals("This is content", post.getContent());
        assertEquals(PostStatus.DRAFT, post.getPostStatus());
        assertEquals(0, post.getPostAttachments().size());

        org.assertj.core.api.Assertions.assertThat(post.getUser())
                        .usingRecursiveComparison()
                        .ignoringFields("roles", "createdAt", "lastModifiedAt")
                        .isEqualTo(testUser);
    }

    @Test
    @Tag("addPost")
    @DisplayName("Update an existing post with draft status and different content than the existing one")
    void addPost_should_update_an_existing_draft_post_with_different_content_successfully() {
        Post seed = new Post("This is my content", PostStatus.DRAFT, testUser);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn("This is completely different");
        when(postInputProjection.getPostStatus()).thenReturn("DRAFT");

        Post post = postService.addPost(postInputProjection);

        verify(postRepository).save(any(Post.class));

        assertNotNull(post);
        assertNotNull(post.getId());
        assertEquals(seed.getId(), post.getId());
        assertEquals(seed.getContent(), post.getContent());
        assertEquals(PostStatus.DRAFT, post.getPostStatus());
        assertEquals(0, post.getPostAttachments().size());

        org.assertj.core.api.Assertions.assertThat(post.getUser())
                .usingRecursiveComparison()
                .ignoringFields("roles", "createdAt", "lastModifiedAt")
                .isEqualTo(testUser);
    }

    @Test
    @Tag("addPost")
    @DisplayName("Update an existing draft post using the same content(nothing different in the post), Should return the retrieved post without sending a database request through postRepository for update")
    void addPost_should_successfully_update_post_with_the_same_content_without_saving_to_the_database() {
        Post seed = new Post("This is same content", PostStatus.DRAFT, testUser);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn("This is same content");
        when(postInputProjection.getPostStatus()).thenReturn("DRAFT");

        Post post = postService.addPost(postInputProjection);

        verify(postRepository, never()).save(any(Post.class));

        assertNotNull(post);
        assertNotNull(post.getId());
        assertEquals(seed.getId(), post.getId());
        assertEquals(seed.getContent(), post.getContent());
        assertEquals(PostStatus.DRAFT, post.getPostStatus());
        assertEquals(0, post.getPostAttachments().size());

        org.assertj.core.api.Assertions.assertThat(post.getUser())
                .usingRecursiveComparison()
                .ignoringFields("roles", "createdAt", "lastModifiedAt")
                .isEqualTo(testUser);
    }

    @Test
    @Tag("addPost")
    @DisplayName("Attempt to update a non-draft post to draft status, (should through InvalidInputException)")
    void addPost_attempt_to_update_nondraft_post_to_post_status_should_throw_InvalidInputException() {
        Post seed = new Post("This is same content", PostStatus.PUBLISHED, testUser);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn("This is same content");
        when(postInputProjection.getPostStatus()).thenReturn("DRAFT");

        assertThrows(InvalidInputException.class, () -> postService.addPost(postInputProjection),
                "Cannot draft a non-draft post");
    }

    @ParameterizedTest(name = "content")
    @Tag("addPost")
    @DisplayName("Attempt to update a draft post with empty content (should work properly)")
    @NullSource
    @ValueSource(strings = { "", "   " })
    void addPost_attempt_to_update_draft_post_with_empty_content_should_not_exceptions(String content) {
        Post seed = new Post("My content", PostStatus.DRAFT, testUser);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn(content);
        when(postInputProjection.getPostStatus()).thenReturn("DRAFT");

        assertDoesNotThrow(() -> postService.addPost(postInputProjection));
    }

    @ParameterizedTest(name = "content")
    @ValueSource(strings = { "", "   " })
    @NullSource
    @Tag("addPost")
    @DisplayName("Attempt to update a draft post with empty content (should work properly)")
    void addPost_should_throw_ForbiddenPostAccessException(String content) {
        Post seed = new Post("My content", PostStatus.DRAFT, testUser1);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn(content);
        when(postInputProjection.getPostStatus()).thenReturn("DRAFT");

        assertThrows(ForbiddenPostAccessException.class, () -> postService.addPost(postInputProjection));
    }

    @Test
    @Tag("addPost")
    @DisplayName("Archive a post with no id provided (null), should through InvalidInputException")
    void addPost_no_post_id_should_through_InvalidInputException() {
        when(postInputProjection.getId()).thenReturn(null);
        when(postInputProjection.getContent()).thenReturn("My content");
        when(postInputProjection.getPostStatus()).thenReturn("ARCHIVED");

        assertThrows(InvalidInputException.class, () -> postService.addPost(postInputProjection), "Cannot archive a non-existing post");
    }

    @Test
    @Tag("addPost")
    @DisplayName("Attempt to archive a non-existing post (should through PostNotFoundException)")
    void addPost_archive_post_should_through_PostNotFoundException() {
        Long nonExistingPostId = 95L;

        when(postInputProjection.getId()).thenReturn(nonExistingPostId);
        when(postInputProjection.getContent()).thenReturn("My content");
        when(postInputProjection.getPostStatus()).thenReturn("ARCHIVED");

        assertThrows(PostNotFoundException.class, () -> postService.addPost(postInputProjection));

    }

    @Test
    @Tag("addPost")
    @DisplayName("Not post owner attempts to archive a post (should through ForbiddenPostAccessException)")
    void addPost_should_throw_ForbiddenPostAccessException() {
        Post seed = new Post("My content", PostStatus.PUBLISHED, testUser1);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn("My content");
        when(postInputProjection.getPostStatus()).thenReturn("ARCHIVED");

        assertThrows(ForbiddenPostAccessException.class, () -> postService.addPost(postInputProjection));
    }

    @Test
    @Tag("addPost")
    @DisplayName("Attempt to archive a draft post (should throw InvalidInputException)")
    void addPost_archive_a_draft_post_should_throw_InvalidInputException() {
        Post seed = new Post("My content", PostStatus.DRAFT, testUser);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn("My content");
        when(postInputProjection.getPostStatus()).thenReturn("ARCHIVED");

        assertThrows(InvalidInputException.class, () -> postService.addPost(postInputProjection),
                "Cannot archive a draft post");

    }

    @Test
    @Tag("addPost")
    @DisplayName("Attempt to archive a published post (should complete successfully)")
    void addPost_archive_a_published_post_should_complete_successfully() {
        Post seed = new Post("My content", PostStatus.PUBLISHED, testUser);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn("My content");
        when(postInputProjection.getPostStatus()).thenReturn("ARCHIVED");

        Post actual = postService.addPost(postInputProjection);

        verify(postRepository).save(any(Post.class));

        org.assertj.core.api.Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("user", "postAttachments")
                .isEqualTo(seed);
    }

    @Test
    @Tag("addPost")
    @DisplayName("Attempt to archive an archived post (should complete successfully and not call postRepostory.save())")
    void addPost_archive_an_archived_post_should_complete_successfully_and_not_call_postRepository_save() {
        Post seed = new Post("My content", PostStatus.ARCHIVED, testUser);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn("My content");
        when(postInputProjection.getPostStatus()).thenReturn("ARCHIVED");

        Post actual = postService.addPost(postInputProjection);

        verify(postRepository, never()).save(any(Post.class));

        org.assertj.core.api.Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("user", "postAttachments")
                .isEqualTo(seed);
    }

    @ParameterizedTest(name = "content")
    @NullSource
    @ValueSource(strings = {"", "  "})
    @Tag("addPost")
    void addPost_publish_new_post_with_blank_content_should_throw_InvalidInputException(String content) {
        when(postInputProjection.getId()).thenReturn(null);
        when(postInputProjection.getContent()).thenReturn(content);
        when(postInputProjection.getPostStatus()).thenReturn("PUBLISHED");

        assertThrows(InvalidInputException.class, () -> postService.addPost(postInputProjection), "Post must have non-empty content");
    }

    @Test
    @Tag("addPost")
    void addPost_publish_new_valid_post_should_save_post_successfully() {
        when(postInputProjection.getId()).thenReturn(null);
        when(postInputProjection.getContent()).thenReturn("This is my content");
        when(postInputProjection.getPostStatus()).thenReturn("PUBLISHED");
        when(kafkaTemplate.send(anyString(), anyString(), any(Object.class))).thenReturn(null);

        Post actual = assertDoesNotThrow(() -> postService.addPost(postInputProjection));

        verify(postRepository).save(any(Post.class));

        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertTrue(actual.getId() > 0L);

        assertEquals("This is my content", actual.getContent());
        assertEquals(PostStatus.PUBLISHED, actual.getPostStatus());
        org.assertj.core.api.Assertions.assertThat(actual.getUser())
                                            .usingRecursiveComparison()
                        .ignoringFields("roles", "createdAt", "lastModifiedAt")
                                            .isEqualTo(testUser);
    }

    @Test
    @Tag("addPost")
    void addPost_publish_an_existing_post_should_save_successfully() {
        Post seed = new Post("My published content", PostStatus.DRAFT, testUser);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn("This is my content");
        when(postInputProjection.getPostStatus()).thenReturn("PUBLISHED");

        Post actual = assertDoesNotThrow(() -> postService.addPost(postInputProjection));
        verify(postRepository).save(any(Post.class));

        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertEquals(seed.getId(), actual.getId());
        assertEquals("This is my content", actual.getContent());
        assertEquals(PostStatus.PUBLISHED, actual.getPostStatus());
    }

    @Test
    @Tag("addPost")
    void addPost_publish_post_with_non_null_id_but_does_not_exist_should_throw_PostNotFoundException() {
        Long nonExistingId = 95L;
        when(postInputProjection.getId()).thenReturn(nonExistingId);
        when(postInputProjection.getContent()).thenReturn("This is my content");
        when(postInputProjection.getPostStatus()).thenReturn("PUBLISHED");

        assertThrows(PostNotFoundException.class, () -> postService.addPost(postInputProjection));
    }

    @Test
    @Tag("addPost")
    void addPost_publish_post_with_non_null_id_but_does_not_belong_to_the_authenticated_user_should_throw_ForbiddenPostAccessException() {
        Post seed = new Post("My published content", PostStatus.DRAFT, testUser1);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn("This is my content");
        when(postInputProjection.getPostStatus()).thenReturn("PUBLISHED");

        assertThrows(ForbiddenPostAccessException.class, () -> postService.addPost(postInputProjection),
                "Forbidden: Cannot access post with id " + seed.getId());

        // verify(postRepository).findById(seed.getId());
    }

    @ParameterizedTest(name = "content")
    @NullSource
    @ValueSource(strings = { "", "   " })
    @Tag("addPost")
    void addPost_publish_post_with_new_blank_content_and_no_attachments_should_Throw_InvalidInputException(
            String content) {
        Post seed = new Post("My published content", PostStatus.DRAFT, testUser);
        seed = postRepository.save(seed);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn(content);
        when(postInputProjection.getPostStatus()).thenReturn("PUBLISHED");

        assertThrows(InvalidInputException.class, () -> postService.addPost(postInputProjection),
                "Post must at least have non-blank content or uploaded attachments");

        // verify(postRepository).findById(anyLong());
    }

    private PostAttachment createPostAttachment(Post post, AttachmentStatus status) {
        Attachment attachment = new Attachment();
        attachment.setName("my_name");
        attachment.setStatus(status);
        attachment.setSize(500L);
        attachment.setUrl(storagePath + File.separator + "test.png");
        attachment.setExtension("png");
        attachment.setAttachmentType(AttachmentType.IMAGE);
        attachment = attachmentRepository.save(attachment);

        PostAttachmentId postAttachmentId = new PostAttachmentId();
        postAttachmentId.setPost(post);
        postAttachmentId.setAttachment(attachment);

        PostAttachment postAttachment = new PostAttachment(postAttachmentId);
        postAttachment = postAttachmentRepository.save(postAttachment);

        post.getPostAttachments().add(postAttachment);
        post = postRepository.save(post);

        reset(attachmentRepository);
        reset(postAttachmentRepository);
        reset(postRepository);
        return postAttachment;
    }

    @ParameterizedTest(name = "content")
    @NullSource
    @ValueSource(strings = { "", "   " })
    @Tag("addPost")
    void addPost_publish_post_with_blank_content_and_1_attachment_should_save_successfully(String content) {
        Post seed = new Post(content, PostStatus.DRAFT, testUser);
        seed = postRepository.save(seed);

        createPostAttachment(seed, AttachmentStatus.COMPLETED);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn(content);
        when(postInputProjection.getPostStatus()).thenReturn("PUBLISHED");

        Post actual = assertDoesNotThrow(() -> postService.addPost(postInputProjection));

        verify(postRepository).save(any(Post.class));

        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertTrue(actual.getId() > 0L);
        assertEquals(content, actual.getContent());
        assertEquals(PostStatus.PUBLISHED, actual.getPostStatus());
        assertEquals(1, actual.getPostAttachments().size());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   ", "This is my content", "   content" })
    @Tag("addPost")
    void addPost_publish_post_without_modifying_the_saved_one_should_return_without_calling_save(String content) {
        Post seed = new Post(content, PostStatus.PUBLISHED, testUser);
        seed = postRepository.save(seed);
        createPostAttachment(seed, AttachmentStatus.COMPLETED);
        reset(postRepository);

        when(postInputProjection.getId()).thenReturn(seed.getId());
        when(postInputProjection.getContent()).thenReturn(content);
        when(postInputProjection.getPostStatus()).thenReturn("PUBLISHED");

        Post actual = assertDoesNotThrow(() -> postService.addPost(postInputProjection));

        verify(postRepository, never()).save(any(Post.class));

        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertTrue(actual.getId() > 0L);
        assertEquals(content, actual.getContent());
        assertEquals(PostStatus.PUBLISHED, actual.getPostStatus());
        assertEquals(1, actual.getPostAttachments().size());
    }

    @Test
    @Tag("findPost")
    void findPost_post_owner_is_the_actor_should_return_post() {
        Post seed = new Post("My content", PostStatus.PUBLISHED, testUser);
        seed = postRepository.save(seed);
        reset(postRepository);

        final Long id = seed.getId();
        Post post = assertDoesNotThrow(() -> postService.findPost(id));
        assertNotNull(post);
    }

    @Test
    @Tag("findPost")
    void findPost_null_postId_should_throw_InvalidInputException() {
        assertThrows(InvalidInputException.class, () -> postService.findPost(null), "Id cannot be null");
    }

    @Test
    @Tag("findPost")
    void findPost_non_existing_post_should_throw_PostNotFoundException() {
        System.out.println(((CustomUserDetails) SecurityUtils.getPrincipal()));
        Long nonExistingId = 95L;
        assertThrows(PostNotFoundException.class, () -> postService.findPost(nonExistingId));
    }

    @Test
    @Tag("findPost")
    void findPost_not_friend_and_not_owner_should_throw_ForbiddenPostAccessException() {
        Post seed = new Post("My content", PostStatus.PUBLISHED, testUser1);
        seed = postRepository.save(seed);
        reset(postRepository);

        Long postId = seed.getId();
        assertThrows(ForbiddenPostAccessException.class, () -> postService.findPost(postId),
                "Forbidden: Cannot access post with id " + 1L);
    }
}
