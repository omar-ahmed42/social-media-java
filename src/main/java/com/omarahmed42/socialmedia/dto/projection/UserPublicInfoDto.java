package com.omarahmed42.socialmedia.dto.projection;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class UserPublicInfoDto implements Serializable {
    protected Long id;
    protected String firstName;
    protected String lastName;
    protected String bio;
    protected String username;
    protected LocalDate dateOfBirth;
    protected LocalDateTime createdAt;
    protected AttachmentDto avatar;
    protected AttachmentDto coverPicture;

    public UserPublicInfoDto(Long id, String firstName, String lastName, String bio, String username, LocalDate dateOfBirth, LocalDateTime createdAt, AttachmentDto avatar, AttachmentDto coverPicture) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bio = bio;
        this.username = username;
        this.dateOfBirth = dateOfBirth;
        this.createdAt = createdAt;
        this.avatar = avatar;
        this.coverPicture = coverPicture;
    }
}
