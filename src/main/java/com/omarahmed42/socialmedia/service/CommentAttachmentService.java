package com.omarahmed42.socialmedia.service;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.web.multipart.MultipartFile;

public interface CommentAttachmentService {
    Long saveCommentAttachment(MultipartFile multipartFile, Long commentId);

    void consume(ConsumerRecord<String, Map<String, Object>> consumerRecord);
    void removeCommentAttachment(Long commentId, Long attachmentId);
}
