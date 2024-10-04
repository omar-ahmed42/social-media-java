package com.omarahmed42.socialmedia.controller;

import java.util.List;
import java.util.Set;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.omarahmed42.socialmedia.dto.CursorBasedPagination;
import com.omarahmed42.socialmedia.dto.SortablePaginationInfo;
import com.omarahmed42.socialmedia.dto.response.MessageDto;
import com.omarahmed42.socialmedia.enums.SortOrder;
import com.omarahmed42.socialmedia.mapper.MessageMapper;
import com.omarahmed42.socialmedia.model.Conversation;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.projection.ConversationDetailsProjection;
import com.omarahmed42.socialmedia.service.ConversationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageMapper messageMapper;

    @MutationMapping
    public Conversation createConversationWithMembers(
            @Argument ConversationDetailsProjection conversationDetailsProjection,
            @Argument Set<Long> membersIds) {
        return conversationService.addConversation(conversationDetailsProjection, membersIds);
    }

    @SchemaMapping(typeName = "Conversation", field = "messages")
    public List<MessageDto> messages(Conversation conversation, @Argument Integer pageSize, @Argument String cursor) {
        return messageMapper
                .toMessageDtoList(
                        conversationService.getMessagesBy(conversation, new CursorBasedPagination(pageSize, cursor)));
    }

    @SchemaMapping(typeName = "Conversation", field = "users")
    public List<User> users(Conversation conversation) {
        return conversationService.getUsersBy(conversation);
    }

    @QueryMapping
    public List<Conversation> getConversations(@Argument Integer page, @Argument Integer pageSize, @Argument Long after,
            @Argument SortOrder sort) {
        return conversationService.getConversations(new SortablePaginationInfo(page, pageSize, sort), after);
    }

    @QueryMapping
    public Conversation getConversation(@Argument Long id) {
        return conversationService.getConversation(id);
    }
}
