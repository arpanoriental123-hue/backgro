package com.lakshy.blog.controllers;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;

import com.lakshy.blog.config.AppConstants;
import com.lakshy.blog.exceptions.ApiException;
import com.lakshy.blog.payloads.PostDto;
import com.lakshy.blog.payloads.PostResponse;
import com.lakshy.blog.services.FileService;
import com.lakshy.blog.services.PostService;

@Validated
@RestController
@RequestMapping("/api")
public class PostController {

	private static final org.slf4j.Logger log =
			org.slf4j.LoggerFactory.getLogger(PostController.class);

	@Autowired
	private PostService postService;

	@Autowired
	private FileService fileService;

	@Value("${project.image}")
	private String PATH;

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/users/{userId}/categories/{categoryId}/posts")
	public ResponseEntity<PostDto> createPost(
			@Valid @RequestBody PostDto postDto,
			@PathVariable Integer userId,
			@PathVariable Integer categoryId,
			Authentication authentication)
	{
		PostDto savedPost = postService.createPost(postDto, userId, categoryId, authentication.getName());
		return ResponseEntity.status(HttpStatus.CREATED).body(savedPost);
	}

	@GetMapping("/categories/{categoryId}/posts")
	public ResponseEntity<PostResponse> getPostsByCategory(
			@PathVariable Integer categoryId,
			@RequestParam(value = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)
				@Min(value = 0, message = "pageNumber must not be negative") @Max(value = 10000, message = "pageNumber must not exceed 10000") Integer pageNumber,
			@RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)
				@Min(value = 1, message = "pageSize must be at least 1") @Max(value = 100, message = "pageSize must not exceed 100") Integer pageSize)
	{
		return ResponseEntity.ok(postService.getPostsByCategory(categoryId, pageNumber, pageSize));
	}

	@GetMapping("/users/{userId}/posts")
	public ResponseEntity<PostResponse> getPostsByUser(
			@PathVariable Integer userId,
			@RequestParam(value = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)
				@Min(value = 0, message = "pageNumber must not be negative") @Max(value = 10000, message = "pageNumber must not exceed 10000") Integer pageNumber,
			@RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)
				@Min(value = 1, message = "pageSize must be at least 1") @Max(value = 100, message = "pageSize must not exceed 100") Integer pageSize)
	{
		return ResponseEntity.ok(postService.getPostsByUsers(userId, pageNumber, pageSize));
	}

	@GetMapping("/posts/{postId}")
	public ResponseEntity<PostDto> getPostById(@PathVariable Integer postId) {
		return ResponseEntity.ok(postService.getPostById(postId));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/posts/{postId}")
	public ResponseEntity<Void> deletePost(@PathVariable Integer postId) {
		postService.deletePost(postId);
		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("isAuthenticated()")
	@PutMapping("/posts/{postId}")
	public ResponseEntity<PostDto> updatePost(
			@Valid @RequestBody PostDto postDto,
			@PathVariable Integer postId,
			Authentication authentication)
	{
		return ResponseEntity.ok(postService.updatePost(postDto, postId, authentication.getName()));
	}

	@GetMapping("/posts")
	public ResponseEntity<PostResponse> getAllPostsByPage(
			@RequestParam(value = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)
				@Min(value = 0, message = "pageNumber must not be negative") @Max(value = 10000, message = "pageNumber must not exceed 10000") Integer pageNumber,
			@RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)
				@Min(value = 1, message = "pageSize must be at least 1") @Max(value = 100, message = "pageSize must not exceed 100") Integer pageSize,
			@RequestParam(value = "sortBy", defaultValue = AppConstants.SORT_BY, required = false)
				@Pattern(regexp = "^(?i)(postId|title|addedDate)$", message = "sortBy must be one of: postId, title, addedDate") String sortBy,
			@RequestParam(value = "sortDir", defaultValue = AppConstants.SORT_DIR, required = false)
				@Pattern(regexp = "(?i)asc|desc", message = "sortDir must be 'asc' or 'desc'") String sortDir,
			@RequestParam(value = "q", required = false)
				@Size(max = 100, message = "Search keyword must not exceed 100 characters") String query)
	{
		if (query != null && !query.isBlank()) {
			return ResponseEntity.ok(postService.searchPostPaginated(query, pageNumber, pageSize, sortBy, sortDir));
		}
		return ResponseEntity.ok(postService.getAllPostsByPage(pageNumber, pageSize, sortBy, sortDir));
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/posts/{postId}/image")
	public ResponseEntity<PostDto> uploadPostImage(
			@RequestParam("image") MultipartFile image,
			@PathVariable Integer postId,
			Authentication authentication) throws IOException
	{
		if (image == null || image.isEmpty()) {
			throw new ApiException("Uploaded file is empty or missing");
		}
		return ResponseEntity.ok(postService.uploadAndAttachImage(postId, image, PATH, authentication.getName()));
	}

	/**
	 * @deprecated Use GET /api/posts/{postId}/image instead. Will be removed after 2026-12-31.
	 */
	@Deprecated
	@GetMapping(value = "/posts/images/{imageName}")
	public ResponseEntity<byte[]> serveImage(
			@Size(max = 255, message = "Image name must not exceed 255 characters")
			@PathVariable String imageName) throws IOException
	{
		log.warn("Deprecated endpoint GET /api/posts/images/{} called. Migrate to GET /api/posts/{{postId}}/image", imageName);

		InputStream resource = fileService.getResources(PATH, imageName);
		byte[] imageBytes = org.springframework.util.StreamUtils.copyToByteArray(resource);

		String extension = imageName.contains(".")
				? imageName.substring(imageName.lastIndexOf('.') + 1).toLowerCase()
				: "";
		MediaType contentType;
		switch (extension) {
			case "png":  contentType = MediaType.IMAGE_PNG;   break;
			case "jpg":
			case "jpeg": contentType = MediaType.IMAGE_JPEG;  break;
			default:     contentType = MediaType.APPLICATION_OCTET_STREAM;
		}

		return ResponseEntity.ok()
				.contentType(contentType)
				.header("Deprecation", "true")
				.header("Sunset", "Thu, 31 Dec 2026 23:59:59 GMT")
				.header("Link", "</api/posts/{postId}/image>; rel=\"successor-version\"")
				.body(imageBytes);
	}

	/**
	 * GET /api/posts/{postId}/image
	 * Serves the image attached to the specified post.
	 * Symmetric with POST /api/posts/{postId}/image.
	 */
	@GetMapping("/posts/{postId}/image")
	public ResponseEntity<byte[]> servePostImage(@PathVariable Integer postId) throws IOException {
		PostDto post = postService.getPostById(postId);
		String imageName = post.getImageName();

		InputStream resource = fileService.getResources(PATH, imageName);
		byte[] imageBytes = org.springframework.util.StreamUtils.copyToByteArray(resource);

		String extension = imageName != null && imageName.contains(".")
				? imageName.substring(imageName.lastIndexOf('.') + 1).toLowerCase()
				: "";
		MediaType contentType;
		switch (extension) {
			case "png":  contentType = MediaType.IMAGE_PNG;   break;
			case "jpg":
			case "jpeg": contentType = MediaType.IMAGE_JPEG;  break;
			default:     contentType = MediaType.APPLICATION_OCTET_STREAM;
		}
		return ResponseEntity.ok().contentType(contentType).body(imageBytes);
	}
}
