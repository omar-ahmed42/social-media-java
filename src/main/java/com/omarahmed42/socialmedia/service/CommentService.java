package com.omarahmed42.socialmedia.service;

import java.util.List;

import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.CommentAttachment;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.projection.CommentInputProjection;

public interface CommentService {
    Comment findComment(Long commentId);

    Comment saveComment(CommentInputProjection commentInputProjection);

    Boolean deleteComment(Long id);

    Post getPostBy(Comment comment);

    List<CommentAttachment> getCommentAttachmentsBy(Comment comment);
}
