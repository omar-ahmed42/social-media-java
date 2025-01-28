package com.omarahmed42.socialmedia.dto.projection;

import java.io.Serializable;

import com.omarahmed42.socialmedia.enums.AttachmentType;

import lombok.Getter;

@Getter
public class AttachmentDto implements Serializable {
    private Long id;
    private String url;
    private AttachmentType type;

    public AttachmentDto(Long id, String url) {
        this.id = id;
        this.url = url;
    }

    public AttachmentDto(Long id, String url, AttachmentType type) {
        this.id = id;
        this.url = url;
        this.type = type;
    }
}
