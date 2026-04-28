package com.lakshy.blog.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.lakshy.blog.exceptions.ApiException;
import com.lakshy.blog.exceptions.ForbiddenException;
import com.lakshy.blog.exceptions.ResourceNotFoundException;
import com.lakshy.blog.payloads.UpdateUserDto;
import com.lakshy.blog.config.AppConstants;
import com.lakshy.blog.entities.Role;
import com.lakshy.blog.entities.User;
import com.lakshy.blog.payloads.UserDto;
import com.lakshy.blog.repositories.RoleRepo;
import com.lakshy.blog.repositories.UserRepo;
import com.lakshy.blog.services.UserService;

@Service
public class UserServiceImpl implements UserService {
	
	@Autowired
	private UserRepo userRepo;
	
	// we have a bean in main springboot class
	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private RoleRepo roleRepo;
	
	// since we are using UserDto so we need to convert it
	// we can also use Modern mapper library instead of these two conversion methods
	// convert UserDto to User
	private User dtoToUser(UserDto userDto) 
	{
		User user = this.modelMapper.map(userDto, User.class);
		/*		User user = new User();
		user.setId(userDto.getId());
		user.setName(userDto.getName());
		user.setEmail(userDto.getEmail());
		user.setAbout(userDto.getAbout());
		user.setPassword(userDto.getPassword()); */
		
		return user;
	}
	
	// Convert User to UserDto
	private UserDto userToDto(User user)
	{
		UserDto userDto = this.modelMapper.map(user, UserDto.class);
		/*UserDto userDto = new UserDto();
		userDto.setAbout(user.getAbout());
		userDto.setId(user.getId());
		userDto.setName(user.getName());
		userDto.setEmail(user.getEmail());
		userDto.setPassword(user.getPassword());*/
		return userDto;
	}

	@Override
	public UserDto createUser(UserDto userDto) {

		User user = this.dtoToUser(userDto);
		// Encode password before persisting — mirrors registerNewUser behaviour
		user.setPassword(this.passwordEncoder.encode(userDto.getPassword()));

		// SECURITY: Never trust client-supplied roles or comments.
		// Clear anything ModelMapper may have copied from the DTO before saving.
		user.setRoles(new java.util.HashSet<>());
		user.setComments(new java.util.ArrayList<>());
		Role defaultRole = roleRepo.findById(AppConstants.NORMAL_USER)
				.orElseThrow(() -> new RuntimeException(
					"Default role NORMAL_USER (id=" + AppConstants.NORMAL_USER + ") not found in database."));
		user.getRoles().add(defaultRole);

		User savedUser = userRepo.save(user);
		return this.userToDto(savedUser);
	}

	@Override
	public UserDto updateUser(UpdateUserDto userDto, Integer userId, String requesterUsername) {
		User requester = userRepo.findByEmail(requesterUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", requesterUsername));
		boolean isAdmin = requester.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		if (!isAdmin && !requester.getId().equals(userId)) {
			throw new ForbiddenException("You are not authorised to update another user's profile");
		}

		User user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User","Id",userId));

		// Prevent duplicate email: check if the new email is already taken by a different user.
		String newEmail = userDto.getEmail();
		if (newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail())) {
			userRepo.findByEmail(newEmail).ifPresent(existing -> {
				if (!existing.getId().equals(userId)) {
					throw new ApiException("Email address is already in use by another account");
				}
			});
		}

		user.setName(userDto.getName());
		user.setEmail(newEmail);
		user.setAbout(userDto.getAbout());
		// BCrypt-encode the password only when a new one is explicitly provided.
		if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
			user.setPassword(this.passwordEncoder.encode(userDto.getPassword()));
		}

		// SECURITY: Roles are managed by admin actions only — never from a user-supplied DTO.

		User updatedUser = userRepo.save(user);
		return this.userToDto(updatedUser);
	}

	@Override
	public UserDto getUserById(Integer userId) {
		User user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User","Id",userId));
		UserDto userDto = this.userToDto(user);
		return userDto;
	}

	@Override
	public List<UserDto> getAllUsers() {
		List<User> userList = userRepo.findAll();
//		List<UserDto> userDtoList = new ArrayList<>();
//		for(User u : userList) {
//			userDtoList.add(this.userToDto(u));
//		}
		// or
		List<UserDto> userDtoList = userList.stream().map(user -> this.userToDto(user)).collect(Collectors.toList());
		return userDtoList;
	}

	@Override
	public void deleteUser(Integer userId) {
		User user = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User","Id",userId));
		userRepo.delete(user);

	}

	@Override
	public UserDto getUserByUsername(String username) {
		User user = userRepo.findByEmail(username)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", username));
		return this.userToDto(user);
	}

	@Override
	public UserDto registerNewUser(UserDto userDto) {
		// Delegate to createUser so role assignment, password encoding, and
		// mass-assignment protection are handled in one place.
		return this.createUser(userDto);
	}

}
