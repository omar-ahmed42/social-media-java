package com.omarahmed42.socialmedia.dto.request;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest implements Serializable {
    @Size(min = 1, max = 50, message = "First name must between 1 and 50 characters.") @NotBlank(message = "First Name must not be blank.") private String firstName;
    @Size(min = 1, max = 50, message = "Last name must between 1 and 50 characters.") @NotBlank(message = "Last Name must not be blank.") private String lastName;
    @Size(min = 1, max = 50, message = "Username must between 1 and 50 characters.") @NotBlank(message = "Username must not be blank.") private String username;
    @NotNull(message = "Email must not be blank.") @Email(message = "Invalid email.") private String email;
    @NotBlank(message = "Password must not be blank.") private String password;
    @Past(message = "Date of birth can only be in the past.") @NotNull(message = "Date of birth must not be blank.") private LocalDate dateOfBirth;
}
