package com.omarahmed42.socialmedia.dto.request;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Data;

@Data
public class SignupRequest implements Serializable {
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String password;
    private LocalDate dateOfBirth;
}
