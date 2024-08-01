package com.vipa.medllm.config.securityconfig;

import lombok.AllArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.vipa.medllm.exception.*;
import com.vipa.medllm.model.User;
import com.vipa.medllm.repository.UserRepository;

import java.util.Set;
import java.util.Collections;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

        private UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String usernameOrEmail) {

                User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                                .orElseThrow(() -> new CustomException(
                                                CustomError.USERNAME_NOT_FOUND_ERROR));

                // 编辑用户权限，暂时没有用户权限设定
                Set<GrantedAuthority> authorities = Collections.emptySet();
                return new org.springframework.security.core.userdetails.User(
                                usernameOrEmail,
                                user.getPassword(), authorities);
        }
}
