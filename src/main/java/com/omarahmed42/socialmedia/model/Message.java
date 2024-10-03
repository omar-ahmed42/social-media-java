package com.omarahmed42.socialmedia.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.omarahmed42.socialmedia.enums.MessageStatus;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Table("messages")
@Data
public class Message implements Persistable<MessagePK>, Serializable {
    @PrimaryKey
    @JsonUnwrapped
    private MessagePK id;

    @Column(value = "content")
    private String content;

    @Column(value = "message_status")
    @Enumerated(EnumType.STRING)
    private MessageStatus messageStatus;

    @Column(value = "user_id")
    private Long userId;

    @Column(value = "attachment_url")
    private String attachmentUrl;

    @CreatedDate
    @Column(value = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(value = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Transient
    @Setter(value = AccessLevel.NONE)
    private transient boolean isNew = true;

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

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    @PrePersist
    @PostLoad
    public void markNotNew() {
        this.isNew = false;
    }

    public void markNew() {
        this.isNew = true;
    }
}
