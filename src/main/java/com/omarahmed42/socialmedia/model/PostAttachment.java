package com.omarahmed42.socialmedia.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode (callSuper = true)
@Table
@Entity
@NoArgsConstructor
public class PostAttachment extends Auditable {

    @EmbeddedId
    private PostAttachmentId postAttachmentId;

    public PostAttachment(PostAttachmentId postAttachmentId) {
        this.postAttachmentId = postAttachmentId;
    }
}
