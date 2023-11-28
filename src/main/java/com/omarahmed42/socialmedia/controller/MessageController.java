package com.omarahmed42.socialmedia.controller;

import java.util.List;

import org.reactivestreams.Publisher;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.omarahmed42.socialmedia.model.Conversation;
import com.omarahmed42.socialmedia.model.Message;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.service.MessageService;
import com.omarahmed42.socialmedia.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    @PostMapping("/api/v1/conversations/{conversation-id}/message")
    public @ResponseBody Message sendMessageByConversationId(MultipartFile multipartFile,
            @PathVariable("conversation-id") Long conversationId,
            @RequestPart("content") String content) {
        return messageService.addMessage(multipartFile, conversationId, content);
    }

    @PostMapping("/api/v1/conversations/users/{receiver-id}")
    public @ResponseBody Message sendMessageByUserId(MultipartFile multipartFile,
            @PathVariable("receiver-id") Long receiverId,
            @RequestPart("content") String content) {
        return messageService.addPersonalMessage(multipartFile, receiverId, content);
    }

    @SubscriptionMapping(value = "messageReceived")
    public Publisher<Message> receiveMessages() {
        return messageService.receiveMessagesPublisher();
    }

    @SubscriptionMapping(value = "messageSent")
    public Publisher<Message> receiveMessagesFromSpecificConversation(
            @Argument Long conversationId) {
        return messageService.receiveMessagesPublisher(conversationId);
    }

    @SchemaMapping(typeName = "Message", field = "user")
    public User user(Message message) {
        return userService.getUser(message.getUserId());
    }

    @SchemaMapping(typeName = "Message", field = "conversation")
    public Conversation conversation(Message message) {
        return messageService.getConversationBy(message);
    }

    @QueryMapping
    public List<Message> findMessages(@Argument Long conversationId, @Argument Long after, @Argument Long before) {
        return messageService.findMessages(conversationId, after, before);
    }
}
