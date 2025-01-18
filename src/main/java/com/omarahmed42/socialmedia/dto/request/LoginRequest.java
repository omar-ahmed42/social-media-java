package com.omarahmed42.socialmedia.dto.request;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest implements Serializable {
    @NotBlank(message = "Email must not be blank.") @Email(message = "Invalid email.") private String email;
    @NotBlank(message = "Password must not be blank.") private String password;
}
