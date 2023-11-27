package com.omarahmed42.socialmedia.service;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.web.multipart.MultipartFile;

public interface PostAttachmentService {
    Long savePostAttachment(MultipartFile multipartFile, Long postId);

    void consume(ConsumerRecord<String, Map<String, Object>> consumerRecord);
}
