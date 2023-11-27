package com.omarahmed42.socialmedia.service.impl;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.Uuid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.omarahmed42.socialmedia.enums.AttachmentStatus;
import com.omarahmed42.socialmedia.enums.AttachmentType;
import com.omarahmed42.socialmedia.enums.PostStatus;
import com.omarahmed42.socialmedia.exception.AttachmentNotFoundException;
import com.omarahmed42.socialmedia.exception.CommentNotFoundException;
import com.omarahmed42.socialmedia.exception.ForbiddenCommentAccessException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.UnsupportedMediaExtensionException;
import com.omarahmed42.socialmedia.model.Attachment;
import com.omarahmed42.socialmedia.model.Comment;
import com.omarahmed42.socialmedia.model.CommentAttachment;
import com.omarahmed42.socialmedia.model.CommentAttachmentId;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.repository.AttachmentRepository;
import com.omarahmed42.socialmedia.repository.CommentAttachmentRepository;
import com.omarahmed42.socialmedia.repository.CommentRepository;
import com.omarahmed42.socialmedia.service.CommentAttachmentService;
import com.omarahmed42.socialmedia.service.FileService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommentAttachmentServiceImpl implements CommentAttachmentService {

    private final FileService fileService;
    private final CommentRepository commentRepository;
    private final CommentAttachmentRepository commentAttachmentRepository;
    private final AttachmentRepository attachmentRepository;
    private static final String[] IMAGE_EXTENSIONS = { "png", "jpg" };
    private static final String[] VIDEO_EXTENSIONS = { "mp4", "avi", "mkv" };

    private static Map<String, String[]> extensions = new HashMap<>();
    private static final String EXTENSION_SEPARATOR = ".";

    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${storage.path}")
    private String storagePath;

    public CommentAttachmentServiceImpl(CommentRepository commentRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            AttachmentRepository attachmentRepository, FileService fileService,
            CommentAttachmentRepository commentAttachmentRepository) {
        this.commentRepository = commentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.attachmentRepository = attachmentRepository;
        this.commentAttachmentRepository = commentAttachmentRepository;
        this.fileService = fileService;

        extensions.put(AttachmentType.IMAGE.toString(), IMAGE_EXTENSIONS);
        extensions.put(AttachmentType.VIDEO.toString(), VIDEO_EXTENSIONS);
    }

    public Long saveCommentAttachment(MultipartFile multipartFile, Long commentId) {
        SecurityUtils.throwIfNotAuthenticated();

        String fileExtension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());

        if (!isValidExtension(fileExtension))
            throw new UnsupportedMediaExtensionException(fileExtension + " extension is not supported");

        AttachmentType attachmentType = getAttachmentType(fileExtension);
        if (attachmentType == null)
            throw new UnsupportedMediaExtensionException(fileExtension + " extension is not supported");

        Comment comment = commentRepository.findCommentById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        throwIfNotCommentOwner(comment, SecurityUtils.getAuthenticatedUserId());

        Post post = comment.getPost();
        if (post.getPostStatus() != PostStatus.PUBLISHED)
            throw new InvalidInputException("Cannot comment on a non-published post");

        String filename = Uuid.randomUuid().toString() + EXTENSION_SEPARATOR
                + fileExtension;

        String fileUrl = storagePath + File.separator + filename;

        Attachment attachment = new Attachment();
        attachment.setExtension(fileExtension);
        attachment.setName(filename);
        attachment.setSize(multipartFile.getSize());
        attachment.setStatus(AttachmentStatus.UPLOADING);
        attachment.setUrl(fileUrl);
        attachment.setAttachmentType(attachmentType);
        attachment = attachmentRepository.save(attachment);

        storeFile(multipartFile, attachment, commentId);
        return attachment.getId();
    }

    private boolean isValidExtension(String fileExtension) {
        for (Map.Entry<String, String[]> entry : extensions.entrySet()) {
            for (String extension : entry.getValue()) {
                if (fileExtension.equalsIgnoreCase(extension))
                    return true;
            }
        }

        return false;
    }

    private AttachmentType getAttachmentType(String fileExtension) {
        for (Map.Entry<String, String[]> entry : extensions.entrySet()) {
            for (String extension : entry.getValue()) {
                if (fileExtension.equalsIgnoreCase(extension))
                    return AttachmentType.valueOf(entry.getKey());
            }
        }

        return null;
    }

    public void storeFile(MultipartFile multipartFile, Attachment attachment, Long commentId) {
        String fileUrl = attachment.getUrl();
        String requestId = UUID.randomUUID().toString();
        try {
            fileService.copy(multipartFile.getInputStream(), Path.of(fileUrl));
            Map<String, Object> successMessage = buildMessage(commentId, attachment.getId(),
                    AttachmentStatus.COMPLETED);
            kafkaTemplate.send("comment-attachment", requestId, successMessage);
        } catch (Exception e) {
            log.error("Error while saving attachment: {}", e);
            Map<String, Object> failedMessage = buildMessage(commentId, attachment.getId(),
                    AttachmentStatus.FAILED);
            kafkaTemplate.send("comment-attachment", requestId, failedMessage);
        }
    }

    private Map<String, Object> buildMessage(Long commentId, Long attachmentId, AttachmentStatus status) {
        return Map.of("commentId", commentId, "attachmentId", attachmentId,
                "status",
                status.toString());
    }

    @KafkaListener(topics = "comment-attachment")
    public void consume(ConsumerRecord<String, Map<String, Object>> consumerRecord) {
        Map<String, Object> value = consumerRecord.value();
        Long commentId = (Long) value.get("commentId");
        Long attachmentId = (Long) value.get("attachmentId");
        AttachmentStatus status = AttachmentStatus.valueOf("status");

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(AttachmentNotFoundException::new);
        attachment.setStatus(status);
        attachment = attachmentRepository.save(attachment);

        CommentAttachmentId commentAttachmentId = new CommentAttachmentId();
        commentAttachmentId.setAttachment(attachment);
        commentAttachmentId.setComment(commentRepository.getReferenceById(commentId));

        CommentAttachment commentAttachment = new CommentAttachment(commentAttachmentId);
        commentAttachmentRepository.save(commentAttachment);
    }

    private void throwIfNotCommentOwner(Comment comment, Long userId) {
        if (!isCommentOwner(comment, userId))
            throw new ForbiddenCommentAccessException("Forbidden: Cannot access comment with id " + comment.getId());
    }

    private boolean isCommentOwner(Comment comment, Long userId) {
        Long commentOwnerId = comment.getUser().getId();
        return commentOwnerId.equals(userId);
    }
}
