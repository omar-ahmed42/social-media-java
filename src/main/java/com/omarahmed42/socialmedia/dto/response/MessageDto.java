package com.omarahmed42.socialmedia.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.omarahmed42.socialmedia.enums.MessageStatus;

import lombok.Data;

@Data
public class MessageDto implements Serializable {
    private Long id;
    private Long conversationId;
    private String content;
    private MessageStatus messageStatus;
    private Long userId;
    private String attachmentUrl;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", shape = JsonFormat.Shape.STRING)
    private LocalDateTime lastModifiedAt;
}
