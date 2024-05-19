package com.omarahmed42.socialmedia.dto.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
@Getter
public class UserPersonalInfoDto extends UserPublicInfoDto {
    
    private String email;

    public UserPersonalInfoDto(Long id, String firstName, String lastName, LocalDate dateOfBirth, String email, LocalDateTime createdAt) {
        super(id, firstName, lastName, dateOfBirth, createdAt);
        this.email = email;
    }
}
