package com.omarahmed42.socialmedia.model;

import java.io.Serializable;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import com.omarahmed42.socialmedia.enums.TokenStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshToken implements Serializable, Persistable<UUID> {
    @Id
    private UUID id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TokenStatus status;

    @Column(name = "valid_unit")
    private Long validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    @Transient
    @Setter(value = AccessLevel.NONE)
    private boolean isNew = true;

    @PrePersist
    @PostLoad
    public void markNotNew() {
        this.isNew = false;
    }

    public void markNew() {
        this.isNew = true;
    }
}
