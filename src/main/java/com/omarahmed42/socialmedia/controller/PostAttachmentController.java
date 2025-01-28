package com.omarahmed42.socialmedia.controller;

import java.net.URI;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.PostAttachment;
import com.omarahmed42.socialmedia.service.PostAttachmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PostAttachmentController {

    private final PostAttachmentService postAttachmentService;

    @PostMapping("/api/v1/posts/{post-id}/attachments")
    public ResponseEntity<Void> addPostAttachment(MultipartFile multipartFile,
            @PathVariable("post-id") Long postId) {
        Long id = postAttachmentService.savePostAttachment(multipartFile, postId);
        return ResponseEntity.created(URI.create("/api/v1/posts/" + postId + "/attachments/" + id)).build();
    }
    
    @DeleteMapping("/api/v1/posts/{post-id}/attachments/{attachment-id}")
    public ResponseEntity<Void> removePostAttachment(@PathVariable("post-id") Long postId, @PathVariable("attachment-id") Long attachmentId) {
        postAttachmentService.removePostAttachment(postId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    @SchemaMapping(typeName = "PostAttachment", field = "post")
    public Post post(PostAttachment postAttachment) {
        return postAttachment.getPostAttachmentId().getPost();
    }

}
