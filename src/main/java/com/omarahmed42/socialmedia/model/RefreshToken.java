package com.omarahmed42.socialmedia.model;

import java.io.Serializable;
import java.util.UUID;

import com.omarahmed42.socialmedia.enums.TokenStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshToken implements Serializable {
    @Id
    private UUID id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TokenStatus status;

    @Column(name = "valid_unit")
    private Long validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
