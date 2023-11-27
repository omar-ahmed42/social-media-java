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
import com.omarahmed42.socialmedia.exception.ForbiddenPostAccessException;
import com.omarahmed42.socialmedia.exception.InvalidInputException;
import com.omarahmed42.socialmedia.exception.PostNotFoundException;
import com.omarahmed42.socialmedia.exception.UnsupportedMediaExtensionException;
import com.omarahmed42.socialmedia.model.Attachment;
import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.PostAttachment;
import com.omarahmed42.socialmedia.model.PostAttachmentId;
import com.omarahmed42.socialmedia.repository.AttachmentRepository;
import com.omarahmed42.socialmedia.repository.PostAttachmentRepository;
import com.omarahmed42.socialmedia.repository.PostRepository;
import com.omarahmed42.socialmedia.service.FileService;
import com.omarahmed42.socialmedia.service.PostAttachmentService;
import com.omarahmed42.socialmedia.util.SecurityUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PostAttachmentServiceImpl implements PostAttachmentService {

    private final FileService fileService;
    private final PostRepository postRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final AttachmentRepository attachmentRepository;
    private static final String[] IMAGE_EXTENSIONS = { "png", "jpg" };
    private static final String[] VIDEO_EXTENSIONS = { "mp4", "avi", "mkv" };

    private static Map<String, String[]> extensions = new HashMap<>();
    private static final String EXTENSION_SEPARATOR = ".";

    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${storage.path}")
    private String storagePath;

    public PostAttachmentServiceImpl(PostRepository postRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            AttachmentRepository attachmentRepository, PostAttachmentRepository postAttachmentRepository,
            FileService fileService) {
        this.postRepository = postRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.attachmentRepository = attachmentRepository;
        this.postAttachmentRepository = postAttachmentRepository;
        this.fileService = fileService;

        extensions.put(AttachmentType.IMAGE.toString(), IMAGE_EXTENSIONS);
        extensions.put(AttachmentType.VIDEO.toString(), VIDEO_EXTENSIONS);
    }

    public Long savePostAttachment(MultipartFile multipartFile, Long postId) {
        SecurityUtils.throwIfNotAuthenticated();

        String fileExtension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());

        if (!isValidExtension(fileExtension))
            throw new UnsupportedMediaExtensionException(fileExtension + " extension is not supported");

        AttachmentType attachmentType = getAttachmentType(fileExtension);
        if (attachmentType == null)
            throw new UnsupportedMediaExtensionException(fileExtension + " extension is not supported");

        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
        throwIfNotPostOwner(post, SecurityUtils.getAuthenticatedUserId());

        if (post.getPostStatus() == PostStatus.ARCHIVED)
            throw new InvalidInputException("Cannot update an archived post");

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

        storeFile(multipartFile, attachment, postId);
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

    public void storeFile(MultipartFile multipartFile, Attachment attachment, Long postId) {
        String fileUrl = attachment.getUrl();
        String requestId = UUID.randomUUID().toString();
        try {
            fileService.copy(multipartFile.getInputStream(), Path.of(fileUrl));
            Map<String, Object> successMessage = buildMessage(postId, attachment.getId(), AttachmentStatus.COMPLETED);
            kafkaTemplate.send("post-attachment", requestId, successMessage);
        } catch (Exception e) {
            log.error("Error while saving attachment: {}", e);
            Map<String, Object> failedMessage = buildMessage(postId, attachment.getId(), AttachmentStatus.FAILED);
            kafkaTemplate.send("post-attachment", requestId, failedMessage);
        }
    }

    private Map<String, Object> buildMessage(Long postId, Long attachmentId, AttachmentStatus status) {
        return Map.of("postId", postId, "attachmentId", attachmentId,
                "status",
                status.toString());
    }

    @KafkaListener(topics = "post-attachment")
    public void consume(ConsumerRecord<String, Map<String, Object>> consumerRecord) {
        Map<String, Object> value = consumerRecord.value();
        Long postId = (Long) value.get("postId");
        Long attachmentId = (Long) value.get("attachmentId");
        AttachmentStatus status = AttachmentStatus.valueOf("status");

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(AttachmentNotFoundException::new);
        attachment.setStatus(status);
        attachment = attachmentRepository.save(attachment);

        PostAttachmentId postAttachmentId = new PostAttachmentId();
        postAttachmentId.setAttachment(attachment);
        postAttachmentId.setPost(postRepository.getReferenceById(postId));

        PostAttachment postAttachment = new PostAttachment(postAttachmentId);
        postAttachmentRepository.save(postAttachment);
    }

    private void throwIfNotPostOwner(Post post, Long userId) {
        if (!isPostOwner(post, userId))
            throw new ForbiddenPostAccessException("Forbidden: Cannot access post with id " + post.getId());
    }

    private boolean isPostOwner(Post post, Long userId) {
        Long postOwnerId = post.getUser().getId();
        return postOwnerId.equals(userId);
    }
}
