package com.lakshy.blog.services;

import com.lakshy.blog.payloads.JwtAuthRequest;
import com.lakshy.blog.payloads.JwtAuthResponse;
import com.lakshy.blog.payloads.RegisterResponseDto;
import com.lakshy.blog.payloads.UserDto;

public interface AuthService {
    JwtAuthResponse login(JwtAuthRequest request);
    RegisterResponseDto register(UserDto userDto);
}
