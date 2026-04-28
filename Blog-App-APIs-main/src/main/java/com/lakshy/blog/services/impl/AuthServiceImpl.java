package com.lakshy.blog.services.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.lakshy.blog.exceptions.ForbiddenException;
import com.lakshy.blog.exceptions.UnauthorizedException;
import com.lakshy.blog.payloads.JwtAuthRequest;
import com.lakshy.blog.payloads.JwtAuthResponse;
import com.lakshy.blog.payloads.RegisterResponseDto;
import com.lakshy.blog.payloads.UserDto;
import com.lakshy.blog.security.JwtTokenHelper;
import com.lakshy.blog.services.AuthService;
import com.lakshy.blog.services.UserService;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtTokenHelper jwtTokenHelper;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public JwtAuthResponse login(JwtAuthRequest request) {
        authenticateCredentials(request.getUsername(), request.getPassword());
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtTokenHelper.generateToken(userDetails);
        JwtAuthResponse response = new JwtAuthResponse();
        response.setToken(token);
        return response;
    }

    @Override
    public RegisterResponseDto register(UserDto userDto) {
        UserDto newUser = userService.registerNewUser(userDto);
        return modelMapper.map(newUser, RegisterResponseDto.class);
    }

    private void authenticateCredentials(String username, String password) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException ex) {
            throw new ForbiddenException("User account is disabled");
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid username or password");
        }
    }
}
