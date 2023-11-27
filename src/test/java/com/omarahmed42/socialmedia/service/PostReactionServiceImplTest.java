package com.omarahmed42.socialmedia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.transaction.annotation.Transactional;

import com.omarahmed42.socialmedia.annotation.WithTestUser;
import com.omarahmed42.socialmedia.enums.PostStatus;
import com.omarahmed42.socialmedia.enums.Roles;
import com.omarahmed42.socialmedia.exception.AuthenticationException;
import com.omarahmed42.socialmedia.exception.ForbiddenPostAccessException;
import com.omarahmed42.socialmedia.exception.PostNotFoundException;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.PostReaction;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.repository.PostReactionRepository;
import com.omarahmed42.socialmedia.repository.PostRepository;
import com.omarahmed42.socialmedia.repository.UserRepository;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
@WithTestUser(setupBefore = TestExecutionEvent.TEST_EXECUTION, username = "test.secret.email0")
class PostReactionServiceImplTest {
    @Autowired
    private PostReactionService postReactionService;

    @SpyBean
    private PostRepository postRepository;

    @SpyBean
    private UserRepository userRepository;

    @SpyBean
    private PostReactionRepository postReactionRepository;

    @MockBean
    private BlockingService blockingService;

    @MockBean
    private FriendService friendService;

    private Post publishedPost;
    private Post publishedPostByTestUser2;
    private Post draftPost;
    private Post draftPostByTestUser2;

    @Value("${test.secret.email0}")
    private String testEmail0;

    @Value("${test.secret.email1}")
    private String testEmail1;

    private User testUser;
    private User testUser2;

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

        draftPost = new Post();
        draftPost.setUser(testUser);
        draftPost.setContent("My content");
        draftPost.setPostStatus(PostStatus.DRAFT);
        draftPost = postRepository.save(draftPost);

        draftPostByTestUser2 = new Post();
        draftPostByTestUser2.setUser(testUser2);
        draftPostByTestUser2.setContent("My content");
        draftPostByTestUser2.setPostStatus(PostStatus.DRAFT);
        draftPostByTestUser2 = postRepository.save(draftPostByTestUser2);

        publishedPostByTestUser2 = new Post();
        publishedPostByTestUser2.setUser(testUser2);
        publishedPostByTestUser2.setContent("My content");
        publishedPostByTestUser2.setPostStatus(PostStatus.PUBLISHED);
        publishedPostByTestUser2 = postRepository.save(publishedPostByTestUser2);
        reset(postRepository);
    }

    @Test
    @WithAnonymousUser
    void savePostReaction_not_authenticated_should_throw_AuthenticationException() {
        Long postId = publishedPost.getId();
        assertThrows(AuthenticationException.class,
                () -> postReactionService.savePostReaction(1, postId));
    }

    @Test
    void savePostReaction_missing_post_Should_throw_PostNotFoundException() {
        assertThrows(PostNotFoundException.class,
                () -> postReactionService.savePostReaction(1, 456L));
    }

    @Test
    void savePostReaction_reactor_blocked_should_throw_ForbiddenPostAccessException() {
        Long postId = publishedPostByTestUser2.getId();
        when(blockingService.isBlocked(anyLong(), anyLong())).thenReturn(true);
        assertThrows(ForbiddenPostAccessException.class, () -> postReactionService.savePostReaction(1, postId),
                "Forbidden: cannot access post with id " + postId);
    }

    @Test
    void savePostReaction_reactor_not_friend_should_throw_ForbiddenPostAccessException() {
        Long postId = publishedPostByTestUser2.getId();
        when(friendService.isFriend(anyLong(), anyLong())).thenReturn(false);
        assertThrows(ForbiddenPostAccessException.class, () -> postReactionService.savePostReaction(1, postId),
                "Forbidden: cannot access post with id " + postId);
    }

    @Test
    void savePostReaction_post_not_public_should_throw_ForbiddenPostAccessException() {
        Long postId = draftPostByTestUser2.getId();
        when(friendService.isFriend(anyLong(), anyLong())).thenReturn(true);
        when(blockingService.isBlocked(anyLong(), anyLong())).thenReturn(false);
        assertThrows(ForbiddenPostAccessException.class, () -> postReactionService.savePostReaction(1, postId),
                "Forbidden: cannot access post with id " + postId);
    }

    @Test
    void savePostReaction_post_owner_and_published_post_should_save_successfully() {
        Long postId = publishedPost.getId();
        PostReaction postReaction = postReactionService.savePostReaction(1, postId);

        verify(postReactionRepository).save(any(PostReaction.class));
        assertNotNull(postReaction);
        assertEquals(1, postReaction.getReaction().getId());
    }

    @Test
    void savePostReaction_not_owner_and_published_post_and_not_blocked_and_friend_should_save_and_return_successfully() {
        when(blockingService.isBlocked(anyLong(), anyLong())).thenReturn(false);
        when(friendService.isFriend(anyLong(), anyLong())).thenReturn(true);

        PostReaction postReaction = postReactionService.savePostReaction(1, publishedPostByTestUser2.getId());
        verify(postReactionRepository).save(any(PostReaction.class));

        assertNotNull(postReaction);
        assertEquals(1, postReaction.getReaction().getId());
        assertNotNull(postReaction.getPostReactionId());
        assertEquals(testUser.getId(), postReaction.getPostReactionId().getUser().getId());
        assertEquals(publishedPostByTestUser2.getId(), postReaction.getPostReactionId().getPost().getId());
    }

}
