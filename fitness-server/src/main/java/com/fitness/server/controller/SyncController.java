package com.fitness.server.controller;

import com.fitness.server.dto.ApiResponse;
import com.fitness.server.dto.SyncRequest;
import com.fitness.server.dto.SyncResponse;
import com.fitness.server.service.SyncService;
import com.fitness.server.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
    
    @Autowired
    private SyncService syncService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping
    public ApiResponse<SyncResponse> sync(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SyncRequest request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(token);
            SyncResponse response = syncService.sync(userId, request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
