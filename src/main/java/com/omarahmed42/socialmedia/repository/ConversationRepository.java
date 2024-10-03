package com.omarahmed42.socialmedia.repository;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Cacheable(cacheNames = "conversations", key = "#id")
    Optional<Conversation> findById(Long id);

    @EntityGraph(attributePaths = { "conversationMembers" })
    Optional<Conversation> findConversationById(Long id);

    @Query(value = """
            SELECT c.id, c.name, c.is_group, c.created_at, c.last_modified_at FROM conversation c WHERE c.is_group = false AND c.id IN (
            SELECT cm1.conversation_id FROM conversation_member cm1 WHERE cm1.user_id = :first_member_id
            AND cm1.conversation_id IN (
            SELECT cm2.conversation_id FROM conversation_member cm2 WHERE cm2.user_id = :second_member_id )
            )
                """, nativeQuery = true)
    Optional<Conversation> findPersonalConversationBy(@Param("first_member_id") Long firstMemberId,
            @Param("second_member_id") Long secondMemberId);

    Page<Conversation> findAllByConversationMembers_User_id(Long userId, PageRequest page);

    Page<Conversation> findAllByConversationMembers_User_idAndIdAfter(Long userId, Long after,
            PageRequest page);
}
