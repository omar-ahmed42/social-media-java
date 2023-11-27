package com.omarahmed42.socialmedia.repository;

import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.Message;
import com.omarahmed42.socialmedia.model.MessageId;

@Repository
public interface MessageRepository extends CassandraRepository<Message, MessageId> {

    // List<Message> findAllByConversationId(Long id);
    List<Message> findAllByIdConversationId(Long id);

    List<Message> findAllByIdConversationIdAndIdMessageIdGreaterThan(Long conversationId, Long after, Pageable pageable);

    // List<Message> findAllByConversationIdAndIdLessThan(Long conversationId, Long before, Pageable pageable);
    List<Message> findAllByIdConversationIdAndIdMessageIdLessThan(Long conversationId, Long before, Pageable pageable);

    List<Message> findAllByIdConversationId(Long conversationId, Pageable pageable);

}
