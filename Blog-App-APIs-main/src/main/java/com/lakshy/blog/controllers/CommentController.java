package com.lakshy.blog.controllers;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.lakshy.blog.payloads.CommentDto;
import com.lakshy.blog.services.CommentService;

/**
 * Handles comment operations. Paths are declared per-method because the two endpoints
 * share no common path prefix: POST /api/posts/{postId}/comments vs DELETE /api/comments/{commentId}.
 */
@RestController
public class CommentController {

	@Autowired
	private CommentService commentService;

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/api/posts/{postId}/comments")
	public ResponseEntity<CommentDto> createComment(
			@Valid @RequestBody CommentDto comment,
			@PathVariable Integer postId,
			Authentication authentication)
	{
		CommentDto created = this.commentService.createComment(comment, postId, authentication.getName());
		return new ResponseEntity<>(created, HttpStatus.CREATED);
	}

	@PreAuthorize("hasRole('ADMIN') or @commentService.isOwner(#commentId, authentication.name)")
	@DeleteMapping("/api/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(@PathVariable Integer commentId) {
		this.commentService.deleteComment(commentId);
		return ResponseEntity.noContent().build();
	}
}
