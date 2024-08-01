package com.vipa.medllm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vipa.medllm.dto.request.user.LoginRequest;
import com.vipa.medllm.dto.request.user.RegisterRequest;
import com.vipa.medllm.dto.response.ResponseResult;
import com.vipa.medllm.service.user.AuthService;
import com.vipa.medllm.service.user.UserService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/user")
public class UserController {
    private AuthService authService;
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ResponseResult<Object>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {

        userService.register(registerRequest);
        ResponseResult<Object> response = new ResponseResult<>(200, "User registered successfully");

        return ResponseEntity.ok(response);

    }

    @PostMapping("/login")
    public ResponseEntity<ResponseResult<String>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String token = authService.login(loginRequest);

        ResponseResult<String> response = new ResponseResult<>(200, "User logged in successfully", token);

        return ResponseEntity.ok(response);
    }
}