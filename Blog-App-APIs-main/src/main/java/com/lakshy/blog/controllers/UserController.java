package com.lakshy.blog.controllers;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import com.lakshy.blog.payloads.PublicUserDto;
import com.lakshy.blog.payloads.UpdateUserDto;
import com.lakshy.blog.payloads.UserDto;
import com.lakshy.blog.services.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired
	private UserService userService;

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping
	public ResponseEntity<PublicUserDto> createUser(@Valid @RequestBody UserDto userDto) {
		return new ResponseEntity<>(PublicUserDto.from(userService.createUser(userDto)), HttpStatus.CREATED);
	}

	@PreAuthorize("isAuthenticated()")
	@PutMapping("/{userId}")
	public ResponseEntity<PublicUserDto> updateUser(
			@Valid @RequestBody UpdateUserDto userDto,
			@PathVariable Integer userId,
			Authentication authentication)
	{
		return ResponseEntity.ok(PublicUserDto.from(
				userService.updateUser(userDto, userId, authentication.getName())));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> deleteUser(@PathVariable Integer userId) {
		userService.deleteUser(userId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{userId}")
	public ResponseEntity<PublicUserDto> getUserById(@PathVariable Integer userId) {
		return ResponseEntity.ok(PublicUserDto.from(userService.getUserById(userId)));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping
	public ResponseEntity<List<PublicUserDto>> getAllUsers() {
		List<PublicUserDto> publicUsers = userService.getAllUsers().stream()
				.map(PublicUserDto::from)
				.collect(Collectors.toList());
		return new ResponseEntity<>(publicUsers, HttpStatus.OK);
	}
}
