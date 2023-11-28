package com.omarahmed42.socialmedia.service;

import java.util.List;
import java.util.Set;

import com.omarahmed42.socialmedia.model.Conversation;
import com.omarahmed42.socialmedia.model.Message;
import com.omarahmed42.socialmedia.model.User;
import com.omarahmed42.socialmedia.projection.ConversationDetailsProjection;

public interface ConversationService {
    Conversation addConversation(Long creatorId, ConversationDetailsProjection details, Set<Long> membersIds);

    Conversation addConversation(ConversationDetailsProjection details, Set<Long> membersIds);

    List<User> getUsersBy(Conversation conversation);

    List<Message> getMessagesBy(Conversation conversation);
}
