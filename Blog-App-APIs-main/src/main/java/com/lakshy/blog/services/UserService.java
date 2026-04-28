package com.lakshy.blog.services;

import java.util.List;

import com.lakshy.blog.payloads.UpdateUserDto;
import com.lakshy.blog.payloads.UserDto;

public interface UserService 
{
	
	UserDto registerNewUser(UserDto user);
	
	UserDto createUser(UserDto user);
	
	UserDto updateUser(UpdateUserDto user, Integer userId, String requesterUsername);
	
	UserDto getUserById(Integer userId);
	
	List<UserDto> getAllUsers();
	
	void deleteUser(Integer userId);

	UserDto getUserByUsername(String username);

}
