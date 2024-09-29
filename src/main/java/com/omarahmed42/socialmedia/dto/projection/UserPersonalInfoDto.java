package com.omarahmed42.socialmedia.dto.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class UserPersonalInfoDto extends UserPublicInfoDto {
    
    private String email;

    public UserPersonalInfoDto(Long id, String firstName, String lastName, String username, LocalDate dateOfBirth, String email, LocalDateTime createdAt, AttachmentDto avatar, AttachmentDto coverPicture) {
        super(id, firstName, lastName, username, dateOfBirth, createdAt, avatar, coverPicture);
        this.email = email;
    }
}
