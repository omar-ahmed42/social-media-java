package com.omarahmed42.socialmedia.controller;

import java.util.List;
import java.util.Set;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.model.Conversation;
import com.omarahmed42.socialmedia.model.Message;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.projection.ConversationDetailsProjection;
import com.omarahmed42.socialmedia.service.ConversationService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @MutationMapping
    public Conversation createConversationWithMembers(
            @Argument ConversationDetailsProjection conversationDetailsProjection,
            @Argument Set<Long> membersIds) {
        return conversationService.addConversation(conversationDetailsProjection, membersIds);
    }

    @SchemaMapping(typeName = "Conversation", field = "messages")
    public List<Message> messages(Conversation conversation) {
        return conversationService.getMessagesBy(conversation);
    }

    @SchemaMapping(typeName = "Conversation", field = "users")
    public List<User> users(Conversation conversation) {
        return conversationService.getUsersBy(conversation);
    }
}
