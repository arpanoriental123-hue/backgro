package com.lakshy.blog.payloads;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for user update requests. Unlike {@link UserDto}, {@code password} is optional —
 * omit it or send blank to leave the existing password unchanged.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateUserDto {

    @NotEmpty
    @Size(min = 4, message = "Username must be minimum of 4 characters")
    private String name;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email address is not valid")
    private String email;

    @NotEmpty
    private String about;

    /**
     * Leave blank to keep the current password.
     * If provided, must satisfy the same complexity rules as registration.
     */
    @JsonProperty(access = Access.WRITE_ONLY)
    @Pattern(
        regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$",
        message = "Password must be blank or at least 8 characters with one uppercase, one lowercase, and one digit"
    )
    @Size(max = 72, message = "Password must not exceed 72 characters")
    private String password;
}
