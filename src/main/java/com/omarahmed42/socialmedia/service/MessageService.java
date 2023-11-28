package com.omarahmed42.socialmedia.service;

import java.util.List;

import org.reactivestreams.Publisher;
import org.springframework.web.multipart.MultipartFile;

import com.omarahmed42.socialmedia.model.Conversation;
import com.omarahmed42.socialmedia.model.Message;

public interface MessageService {
    Message addPersonalMessage(MultipartFile multipartFile, Long receiverId, String content);

    Message addMessage(MultipartFile multipartFile, Long conversationId, String content);

    Publisher<Message> receiveMessagesPublisher();

    Publisher<Message> receiveMessagesPublisher(Long conversationId);

    Conversation getConversationBy(Message message);

    List<Message> findMessages(Long conversationId, Long after, Long before);
}
