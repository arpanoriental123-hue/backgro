package com.lakshy.blog.payloads;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class JwtAuthRequest {

	@NotBlank(message = "Username must not be blank")
	private String username;

	@NotBlank(message = "Password must not be blank")
	private String password;
}
