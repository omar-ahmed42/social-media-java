package com.omarahmed42.socialmedia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.Conversation;
import com.omarahmed42.socialmedia.model.ConversationMember;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    List<ConversationMember> findAllByConversation(Conversation conversation);

}
