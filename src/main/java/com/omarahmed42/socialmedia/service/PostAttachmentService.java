package com.omarahmed42.socialmedia.service;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.web.multipart.MultipartFile;

import com.omarahmed42.socialmedia.model.Post;
import com.omarahmed42.socialmedia.model.PostAttachment;

public interface PostAttachmentService {
    Long savePostAttachment(MultipartFile multipartFile, Long postId);

    void consume(ConsumerRecord<String, Map<String, Object>> consumerRecord);

    List<PostAttachment> findPostAttachmentsByPost(Post post);
}
