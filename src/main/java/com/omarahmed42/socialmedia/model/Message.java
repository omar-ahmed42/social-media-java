package com.omarahmed42.socialmedia.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import com.omarahmed42.socialmedia.enums.MessageStatus;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Table
@Data
public class Message implements Serializable {
    // @PrimaryKey
    // private Long id;
    @PrimaryKey
    private MessageId id;

    @Column(value = "content")
    private String content;

    @Column(value = "message_status")
    @Enumerated(EnumType.STRING)
    private MessageStatus messageStatus;

    @Column(value = "user_id")
    private Long userId;

    @Column(value = "attachment_url")
    private String attachmentUrl;

    // @Column(value = "conversation_id")
    // private Long conversationId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime lastModifiedAt;

    public void setMessageId(Long messageId) {
        id.setMessageId(messageId);
    }

    public Long getMessageId() {
        return id.getMessageId();
    }

    public void setConversationId(Long conversationId) {
        id.setConversationId(conversationId);
    }

    public Long getConversationId() {
        return id.getConversationId();
    }
}
