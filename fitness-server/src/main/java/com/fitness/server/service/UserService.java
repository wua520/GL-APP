package com.fitness.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fitness.server.dto.AuthResponse;
import com.fitness.server.dto.LoginRequest;
import com.fitness.server.dto.RegisterRequest;
import com.fitness.server.entity.User;
import com.fitness.server.mapper.UserMapper;
import com.fitness.server.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    public AuthResponse register(RegisterRequest request) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername());
        if (userMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setCreatedAt(System.currentTimeMillis());
        userMapper.insert(user);
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getNickname());
    }
    
    public AuthResponse login(LoginRequest request) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getNickname());
    }
    
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
    
    public Long getUserIdFromToken(String token) {
        // 移除 "Bearer " 前缀
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        
        // 验证新密码长度
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码长度不能少于6位");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}
