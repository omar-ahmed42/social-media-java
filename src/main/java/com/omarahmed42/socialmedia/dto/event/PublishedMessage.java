package com.omarahmed42.socialmedia.dto.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.omarahmed42.socialmedia.enums.MessageStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishedMessage implements Serializable {
    private Long messageId;
    private String content;
    private Long senderId;
    private String attachmentUrl;
    private Long conversationId;
    private MessageStatus messageStatus;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    @Builder.Default
    private Set<Long> memberIds = new HashSet<>();
}
