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
    protected LocalDate dateOfBirth;
    protected LocalDateTime createdAt;

    public UserPublicInfoDto(Long id, String firstName, String lastName, LocalDate dateOfBirth, LocalDateTime createdAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.createdAt = createdAt;
    }
}
