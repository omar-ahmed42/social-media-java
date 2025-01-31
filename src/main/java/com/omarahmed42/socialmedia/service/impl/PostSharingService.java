package com.omarahmed42.socialmedia.service.impl;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.enums.PostStatus;
import com.omarahmed42.socialmedia.exception.PostNotFoundException;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.repository.PostRepository;
import com.omarahmed42.socialmedia.service.FriendService;
import com.omarahmed42.socialmedia.service.SharingService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostSharingService implements SharingService {

    private final PostRepository postRepository;
    private final FriendService friendService;

    @Override
    public Integer getSharesCount(Long postId) {
        SecurityUtils.throwIfNotAuthenticated();
        Long authUserId = SecurityUtils.getAuthenticatedUserId();

        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
        if (Objects.isNull(post.getUser()) || (!Objects.equals(authUserId, post.getUser().getId())
                && !friendService.isFriend(authUserId, post.getUser().getId()))) {
            log.warn("Illegal access to post {} by user {}", postId, authUserId);
            throw new PostNotFoundException("Post not found");
        } else if (!Objects.equals(authUserId, post.getUser().getId()) && post.getPostStatus() == PostStatus.DRAFT) {
            log.warn("Illegal access to post {} by user {}", postId, authUserId);
            throw new PostNotFoundException("Post not found");
        }

        if (post.getPostStatus() != PostStatus.PUBLISHED)
            return 0;

        return postRepository.countByParentIdAndPostStatusNot(postId, PostStatus.DRAFT);
    }

}
