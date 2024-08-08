package com.vipa.medllm.service.user;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vipa.medllm.dto.request.user.RegisterRequest;
import com.vipa.medllm.exception.CustomError;
import com.vipa.medllm.exception.CustomException;
import com.vipa.medllm.model.User;
import com.vipa.medllm.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest registerRequest) {
        // 检查是否有重复
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new CustomException(CustomError.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new CustomException(CustomError.EMAIL_ALREADY_EXISTS);
        }
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // 检查可以不经过设置的字段是否为空，不允许设置对象对应字段为空，默认使用'N/A'字符串表示空
        if (registerRequest.getPhone() != null) {
            user.setPhone(registerRequest.getPhone());
        }
        if (registerRequest.getProfileLink() != null) {
            user.setProfileLink(registerRequest.getProfileLink());
        }

        userRepository.save(user);
    }

    public User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String usernameOrEmail = userDetails.getUsername();
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail).get();
    }
}
