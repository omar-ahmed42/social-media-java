package com.omarahmed42.socialmedia.model;

import java.util.ArrayList;
import java.util.List;

import com.omarahmed42.socialmedia.enums.PostStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode (callSuper = true)
@Table
@Entity
@NoArgsConstructor
public class Post extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "content", length = 254)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_status")
    private PostStatus postStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @OneToMany(mappedBy = "postAttachmentId.post")
    private List<PostAttachment> postAttachments = new ArrayList<>();

    public Post(String content, PostStatus postStatus, User user) {
        this.content = content;
        this.postStatus = postStatus;
        this.user = user;
    }
}
