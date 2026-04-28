package com.lakshy.blog.services;

import com.lakshy.blog.payloads.CommentDto;

public interface CommentService {
	
	CommentDto createComment(CommentDto commentDto, Integer postId, String username);

	void deleteComment(Integer commentId);

	boolean isOwner(Integer commentId, String username);
	
	
}
