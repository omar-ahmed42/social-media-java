package com.omarahmed42.socialmedia.service;

import com.omarahmed42.socialmedia.model.CommentReaction;
import com.omarahmed42.socialmedia.model.Reaction;

public interface CommentReactionService {
    CommentReaction saveCommentReaction(Integer reactionId, Long commentId);

    CommentReaction removeCommentReaction(Long commentId);

    Reaction getCommentReaction(Long commentId);
}
