package com.omarahmed42.socialmedia.dto.projection;

import java.io.Serializable;

import lombok.Getter;

@Getter
public class AttachmentDto implements Serializable {
    private Long id;
    private String url;

    public AttachmentDto(Long id, String url) {
        this.id = id;
        this.url = url;
    }
}
