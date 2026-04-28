package com.lakshy.blog.services;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.lakshy.blog.payloads.PostDto;
import com.lakshy.blog.payloads.PostResponse;

public interface PostService {
	
	PostDto createPost(PostDto postDto, Integer userId, Integer categoryId, String requesterUsername);

	PostDto updatePost(PostDto postDto, Integer postId, String requesterUsername);
	
	void deletePost(Integer postId);
	
	PostDto getPostById(Integer postId);
	
	// implementing pagination on getPostsByCategory, [view getPostsByUsers implementation for naive approach]
	//get all posts by category
	PostResponse getPostsByCategory(Integer categoryId, Integer pageNumber, Integer pageSize);
	
	// get all posts by User
	PostResponse getPostsByUsers(Integer userId, Integer pageNumber, Integer pageSize);
	
	// search title posts by keyword
	PostResponse searchPostPaginated(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortDir);
	
	 // pagination using JpaRepository -> to send its response we have a seperate PostResponse class in PayLoad
	PostResponse getAllPostsByPage(Integer pageNumber, Integer pageSize, String sortBy, String sortDir);

	PostDto uploadAndAttachImage(Integer postId, MultipartFile image, String imagePath, String requesterUsername) throws IOException;
}
