package com.omarahmed42.socialmedia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.CommentAttachment;
import com.omarahmed42.socialmedia.model.CommentAttachmentId;

@Repository
public interface CommentAttachmentRepository extends JpaRepository<CommentAttachment, CommentAttachmentId> {
    List<CommentAttachment> findAllByCommentAttachmentIdComment(Comment comment);
}
