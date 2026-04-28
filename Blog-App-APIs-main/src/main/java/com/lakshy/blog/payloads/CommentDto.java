package com.lakshy.blog.payloads;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentDto {

	private Integer id;

	@NotBlank(message = "Comment content must not be blank")
	@Size(max = 1000, message = "Comment content must not exceed 1000 characters")
	private String content;

}
