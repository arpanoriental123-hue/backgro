package com.lakshy.blog.services.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.lakshy.blog.config.AppConstants;
import com.lakshy.blog.entities.Category;
import com.lakshy.blog.entities.Post;
import com.lakshy.blog.entities.User;
import com.lakshy.blog.exceptions.ApiException;
import com.lakshy.blog.exceptions.ForbiddenException;
import com.lakshy.blog.exceptions.ResourceNotFoundException;
import com.lakshy.blog.payloads.PostDto;
import com.lakshy.blog.payloads.PostResponse;
import com.lakshy.blog.repositories.CategoryRepo;
import com.lakshy.blog.repositories.PostRepo;
import com.lakshy.blog.repositories.UserRepo;
import com.lakshy.blog.services.FileService;
import com.lakshy.blog.services.PostService;

import org.springframework.web.multipart.MultipartFile;


@Service
public class PostServiceImpl implements PostService {

	@Autowired
	private PostRepo postRepo;
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	private UserRepo userRepo;
	
	@Autowired
	private CategoryRepo categoryRepo;

	@Autowired
	private FileService fileService;
	
	@Override
	public PostDto createPost(PostDto postDto, Integer userId, Integer categoryId, String requesterUsername) {

		User requester = this.userRepo.findByEmail(requesterUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", requesterUsername));
		boolean isAdmin = requester.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		if (!isAdmin && !requester.getId().equals(userId)) {
			throw new ForbiddenException("You are not authorised to create a post for another user");
		}

		User user = this.userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "Id", userId));

		Category category = this.categoryRepo.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category", "Id", categoryId));
		
		Post post = this.modelMapper.map(postDto, Post.class);
		post.setImageName("default.png");
		post.setAddedDate(new Date());
		post.setUser(user);
		post.setCategory(category);
		
		Post savedPost = this.postRepo.save(post);

		return this.modelMapper.map(savedPost, PostDto.class);
	}

	@Override
	public PostDto updatePost(PostDto postDto, Integer postId, String requesterUsername) {
		Post post = this.postRepo.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post", "Id", postId));

		User requester = this.userRepo.findByEmail(requesterUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", requesterUsername));
		boolean isAdmin = requester.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		if (!isAdmin && !post.getUser().getId().equals(requester.getId())) {
			throw new ForbiddenException("You are not authorised to update this post");
		}

		post.setTitle(postDto.getTitle());
		post.setContent(postDto.getContent());
		// imageName is intentionally NOT updated here — images are managed exclusively
		// via POST /api/posts/{postId}/image to prevent arbitrary filename injection.
		
		Post updatedPost = postRepo.save(post);
		
		return this.modelMapper.map(updatedPost, PostDto.class);
	}

	@Override
	public void deletePost(Integer postId) {
		Post post = this.postRepo.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post", "Id", postId));
		postRepo.delete(post);

	}

	@Override
	public PostDto getPostById(Integer postId) {
		Post post = this.postRepo.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post", "Id", postId));
		return this.modelMapper.map(post, PostDto.class);
		
	}

	@Override
	public PostResponse getPostsByCategory(Integer categoryId, Integer pageNumber, Integer pageSize) {
		Pageable pg = PageRequest.of(pageNumber, pageSize);
		
		Category category = this.categoryRepo.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category", "Id", categoryId));
		
		Page<Post> pagePosts = this.postRepo.findByCategory(category, pg);
		List<Post> posts = pagePosts.getContent();
		List<PostDto> postDtos = posts.stream().map((post) -> this.modelMapper.map(post, PostDto.class)).collect(Collectors.toList());
		
		PostResponse postResponse = new PostResponse();
		postResponse.setContent(postDtos);
		postResponse.setPageNumber(pagePosts.getNumber());
		postResponse.setPageSize(pagePosts.getSize());
		postResponse.setTotalElements(pagePosts.getTotalElements());
		postResponse.setTotalPages(pagePosts.getTotalPages());
		postResponse.setLastPage(pagePosts.isLast());	
		
		return postResponse;
	}

	@Override
	public PostResponse getPostsByUsers(Integer userId, Integer pageNumber, Integer pageSize) {
		User user = this.userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "Id", userId));

		Pageable pg = PageRequest.of(pageNumber, pageSize);
		Page<Post> pagePosts = this.postRepo.findByUser(user, pg);
		List<PostDto> postsDtos = pagePosts.getContent().stream()
				.map(post -> this.modelMapper.map(post, PostDto.class))
				.collect(Collectors.toList());

		PostResponse postResponse = new PostResponse();
		postResponse.setContent(postsDtos);
		postResponse.setPageNumber(pagePosts.getNumber());
		postResponse.setPageSize(pagePosts.getSize());
		postResponse.setTotalElements(pagePosts.getTotalElements());
		postResponse.setTotalPages(pagePosts.getTotalPages());
		postResponse.setLastPage(pagePosts.isLast());

		return postResponse;
	}

	@Override
	public PostResponse searchPostPaginated(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortDir) {
		if (!AppConstants.ALLOWED_SORT_FIELDS.contains(sortBy)) {
			throw new ApiException("Invalid sortBy value '" + sortBy + "'. Allowed values: " + AppConstants.ALLOWED_SORT_FIELDS);
		}
		if (!AppConstants.ALLOWED_SORT_DIRS.contains(sortDir.toLowerCase())) {
			throw new ApiException("Invalid sortDir value '" + sortDir + "'. Allowed values: asc, desc");
		}
		Sort sort = sortDir.equalsIgnoreCase("asc")
				? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();
		Pageable pg = PageRequest.of(pageNumber, pageSize, sort);
		Page<Post> pagePosts = this.postRepo.findByTitleContaining(keyword, pg);
		List<PostDto> postDtos = pagePosts.getContent().stream()
				.map(post -> this.modelMapper.map(post, PostDto.class))
				.collect(Collectors.toList());

		PostResponse postResponse = new PostResponse();
		postResponse.setContent(postDtos);
		postResponse.setPageNumber(pagePosts.getNumber());
		postResponse.setPageSize(pagePosts.getSize());
		postResponse.setTotalElements(pagePosts.getTotalElements());
		postResponse.setTotalPages(pagePosts.getTotalPages());
		postResponse.setLastPage(pagePosts.isLast());
		return postResponse;
	}

	// Pagination
	@Override
	public PostResponse getAllPostsByPage(Integer pageNumber, Integer pageSize, String sortBy, String sortDir) {

		if (!AppConstants.ALLOWED_SORT_FIELDS.contains(sortBy)) {
			throw new ApiException("Invalid sortBy value '" + sortBy + "'. Allowed values: " + AppConstants.ALLOWED_SORT_FIELDS);
		}
		if (!AppConstants.ALLOWED_SORT_DIRS.contains(sortDir.toLowerCase())) {
			throw new ApiException("Invalid sortDir value '" + sortDir + "'. Allowed values: asc, desc");
		}

		Sort sort = null;
		if(sortDir.equalsIgnoreCase("asc")) {
			sort = Sort.by(sortBy).ascending();
		}else {
			sort = Sort.by(sortBy).descending();
		}
		
		Pageable pg = PageRequest.of(pageNumber, pageSize, sort);
		
		Page<Post> pagePosts = this.postRepo.findAll(pg);
		List<Post> posts = pagePosts.getContent();
		
		//content
		List<PostDto> postDtos = posts.stream().map((p) -> this.modelMapper.map(p, PostDto.class)).collect(Collectors.toList());
		
		PostResponse postResponse = new PostResponse();
		postResponse.setContent(postDtos);
		postResponse.setPageNumber(pagePosts.getNumber());
		postResponse.setPageSize(pagePosts.getSize());
		postResponse.setTotalElements(pagePosts.getTotalElements());
		postResponse.setTotalPages(pagePosts.getTotalPages());
		postResponse.setLastPage(pagePosts.isLast());		
		
		return postResponse;
	}

	@Override
	public PostDto uploadAndAttachImage(Integer postId, MultipartFile image, String imagePath, String requesterUsername) throws IOException {
		Post post = this.postRepo.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "Id", postId));

		User requester = this.userRepo.findByEmail(requesterUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", requesterUsername));
		boolean isAdmin = requester.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		if (!isAdmin && !post.getUser().getId().equals(requester.getId())) {
			throw new ForbiddenException("You are not authorised to update this post's image");
		}

		String fileName = this.fileService.uploadImage(imagePath, image);
		post.setImageName(fileName);

		Post updatedPost = this.postRepo.save(post);
		return this.modelMapper.map(updatedPost, PostDto.class);
	}

}
