package com.lakshy.blog.payloads;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.lakshy.blog.entities.Comment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class PostDto {

	private Integer postId;

	@NotBlank(message = "Post title must not be blank")
	@Size(min = 1, max = 255, message = "Post title must be between 1 and 255 characters")
	private String title;

	@NotBlank(message = "Post content must not be blank")
	private String content;
	
	@Size(max = 255, message = "Image name must not exceed 255 characters")
	private String imageName;
	
	private Date addedDate;
	
	private CategoryDto category;
	
	private PublicUserDto user;
	
	private Set<CommentDto> comments = new HashSet<>();

}
