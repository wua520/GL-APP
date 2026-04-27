package com.fitness.server.controller;

import com.fitness.server.dto.*;
import com.fitness.server.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = userService.register(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = userService.login(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
