package com.lakshy.blog.payloads;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for the registration endpoint.
 * Returns id, name, and the confirmed email address.
 */
@Getter
@Setter
@NoArgsConstructor
public class RegisterResponseDto {
    private Integer id;
    private String name;
    private String email;
    private String about;
}
