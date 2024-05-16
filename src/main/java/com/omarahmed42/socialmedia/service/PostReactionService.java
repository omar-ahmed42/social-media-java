package com.omarahmed42.socialmedia.service;

import com.omarahmed42.socialmedia.model.PostReaction;

public interface PostReactionService {
    PostReaction savePostReaction(Integer reactionId, Long postId);

    PostReaction removePostReaction(Long postId);
}
