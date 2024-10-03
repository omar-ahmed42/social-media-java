package com.omarahmed42.socialmedia.repository;

import java.util.List;

import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import com.omarahmed42.socialmedia.model.Message;
import com.omarahmed42.socialmedia.model.MessagePK;

@Repository
public interface MessageRepository extends CassandraRepository<Message, MessagePK> {

    List<Message> findAllByIdConversationId(Long id);

    Slice<Message> findAllByIdConversationId(Long id, CassandraPageRequest pageaCassandraPageRequest);

    List<Message> findAllByIdConversationIdAndIdMessageIdGreaterThan(Long conversationId, Long after,
            Pageable pageable);

    List<Message> findAllByIdConversationIdAndIdMessageIdLessThan(Long conversationId, Long before, Pageable pageable);

    List<Message> findAllByIdConversationId(Long conversationId, Pageable pageable);

}
