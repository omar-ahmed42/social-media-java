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

import com.omarahmed42.socialmedia.dto.response.MessageDto;
import com.omarahmed42.socialmedia.mapper.MessageMapper;
import com.omarahmed42.socialmedia.model.Conversation;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.service.MessageService;
import com.omarahmed42.socialmedia.service.UserService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final MessageMapper messageMapper;

    @PostMapping("/api/v1/conversations/{conversation-id}/message")
    public @ResponseBody MessageDto sendMessageByConversationId(MultipartFile multipartFile,
            @PathVariable("conversation-id") Long conversationId,
            @RequestPart("content") String content) {
        return messageMapper.toMessageDto(messageService.addMessage(multipartFile, conversationId, content));
    }

    @PostMapping("/api/v1/conversations/users/{receiver-id}")
    public @ResponseBody MessageDto sendMessageByUserId(MultipartFile multipartFile,
            @PathVariable("receiver-id") Long receiverId,
            @RequestPart("content") String content) {
        return messageMapper.toMessageDto(messageService.addPersonalMessage(multipartFile, receiverId, content));
    }

    @SubscriptionMapping(value = "messageReceived")
    public Publisher<MessageDto> receiveMessages() {
        return Flux.from(messageService.receiveMessagesPublisher()).map(messageMapper::toMessageDto);
    }

    @SubscriptionMapping(value = "messageSent")
    public Publisher<MessageDto> receiveMessagesFromSpecificConversation(
            @Argument Long conversationId) {
        return Flux.from(messageService.receiveMessagesPublisher(conversationId)).map(messageMapper::toMessageDto);
    }

    @SchemaMapping(typeName = "Message", field = "user")
    public User user(MessageDto message) {
        return userService.getUser(message.getUserId());
    }

    @SchemaMapping(typeName = "Message", field = "conversation")
    public Conversation conversation(MessageDto message) {
        return messageService.getConversationBy(message);
    }

    @QueryMapping
    public List<MessageDto> findMessages(@Argument Long conversationId, @Argument Long after, @Argument Long before) {
        return messageMapper.toMessageDtoList(messageService.findMessages(conversationId, after, before));
    }
}
