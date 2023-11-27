package com.omarahmed42.socialmedia.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@Table
@Entity
@NoArgsConstructor
public class CommentReaction extends Auditable {
    @EmbeddedId
    private CommentReactionId commentReactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reaction_id")
    private Reaction reaction;

    public CommentReaction(CommentReactionId commentReactionId) {
        this.commentReactionId = commentReactionId;
    }
}
