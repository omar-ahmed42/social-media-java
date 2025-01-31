package com.omarahmed42.socialmedia.dto.event;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AttachmentDeletionEvent implements Serializable {
    private List<String> attachmentUrls;

    public AttachmentDeletionEvent(List<String> attachmentUrls) {
        this.attachmentUrls = attachmentUrls;
    }
}
