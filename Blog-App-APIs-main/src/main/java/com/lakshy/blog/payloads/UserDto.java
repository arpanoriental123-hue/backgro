package com.lakshy.blog.payloads;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.lakshy.blog.entities.Role;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
// use to transfer data from entities to services
// entities classes are now used to store the data only
// we can expose this DTO classes to apis and can use to get data from user

public class UserDto 
{
	private Integer id;
	
	@NotEmpty
	@Size(min=4,message = "Username must be minimum of 4 characters")
	private String name;
	
	@NotBlank(message = "Email must not be blank")
	@Email(message = "Email address is not valid")
	private String email;
	
	@NotEmpty
	@Size(min=8, max=72, message = "Password must be 8 to 72 characters long")
	@JsonProperty(access = Access.WRITE_ONLY)
	@Pattern(
		regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
		message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
	)
	private String password;
	
	@NotEmpty
	private String about;
	
	@JsonIgnore
	private Set<CommentDto> comments = new HashSet<>();

	@JsonIgnore
	private Set<RoleDto> roles = new HashSet<>();
}
