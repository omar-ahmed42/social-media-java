package com.omarahmed42.socialmedia.service;

import com.omarahmed42.socialmedia.model.PostReaction;
import com.omarahmed42.socialmedia.model.Reaction;

public interface PostReactionService {
    PostReaction savePostReaction(Integer reactionId, Long postId);

    PostReaction removePostReaction(Long postId);

    Reaction getPostReaction(Long postId);
}
