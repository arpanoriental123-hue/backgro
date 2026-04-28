package com.lakshy.blog.payloads;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reduced user representation safe for unauthenticated or cross-user reads.
 * Excludes email, roles, and comments to prevent PII disclosure.
 */
@Getter
@Setter
@NoArgsConstructor
public class PublicUserDto {
    private Integer id;
    private String name;
    private String about;

    public static PublicUserDto from(UserDto u) {
        PublicUserDto p = new PublicUserDto();
        p.id = u.getId();
        p.name = u.getName();
        p.about = u.getAbout();
        return p;
    }
}
