package com.omarahmed42.socialmedia.controller;

import java.net.URI;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.CommentAttachment;
import com.omarahmed42.socialmedia.service.CommentAttachmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CommentAttachmentController {

    private final CommentAttachmentService commentAttachmentService;

    @PostMapping("/api/v1/comments/{comment-id}/attachments")
    public ResponseEntity<Void> addCommentAttachment(MultipartFile multipartFile,
            @PathVariable("comment-id") Long commentId) {
        Long id = commentAttachmentService.saveCommentAttachment(multipartFile, commentId);
        return ResponseEntity.created(URI.create("/api/v1/comments/" + commentId + "/attachments/" + id)).build();
    }

    @DeleteMapping("/api/v1/comments/{comment-id}/attachments/{attachment-id}")
    public ResponseEntity<Void> removeCommentAttachment(@PathVariable("comment-id") Long commentId, @PathVariable("attachment-id") Long attachmentId) {
        commentAttachmentService.removeCommentAttachment(commentId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    @SchemaMapping(typeName = "CommentAttachment", field = "comment")
    public Comment comment(CommentAttachment commentAttachment) {
        return commentAttachment.getCommentAttachmentId().getComment();
    }

}
