package com.omarahmed42.socialmedia.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class Auditable implements Serializable {

    @Column(updatable = false, nullable = false, name = "created_at")
    @CreatedDate
    protected LocalDateTime createdAt;

    @Column(insertable = false, name = "last_modified_at")
    @LastModifiedDate
    protected LocalDateTime lastModifiedAt;
}
