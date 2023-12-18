package com.omarahmed42.socialmedia.model;

import org.hibernate.annotations.GenericGenerator;

import com.omarahmed42.socialmedia.enums.AttachmentStatus;
import com.omarahmed42.socialmedia.enums.AttachmentType;
import com.omarahmed42.socialmedia.generator.SnowflakeUIDGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@Table
@Entity
@NoArgsConstructor
public class Attachment extends Auditable {
    @Id
    @GenericGenerator(name = "snowflake_id_generator", type = SnowflakeUIDGenerator.class)
    @GeneratedValue(generator = "snowflake_id_generator")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "url")
    private String url;

    @Column(name = "size")
    private Long size;

    @Column(name = "extension", length = 25)
    private String extension;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AttachmentStatus status;

    @Column(name = "attachment_type")
    @Enumerated(EnumType.STRING)
    private AttachmentType attachmentType;
}
