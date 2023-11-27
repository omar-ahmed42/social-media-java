package com.omarahmed42.socialmedia.service;

import com.omarahmed42.socialmedia.model.CommentReaction;

public interface CommentReactionService {
    CommentReaction saveCommentReaction(Integer reactionId, Long commentId);
}
