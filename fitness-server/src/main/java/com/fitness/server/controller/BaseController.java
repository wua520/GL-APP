package com.fitness.server.controller;

import com.fitness.server.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 控制器基类 - 统一身份认证处理
 */
public abstract class BaseController {
    
    @Autowired
    protected JwtUtil jwtUtil;
    
    /**
     * 从Authorization头提取并验证用户ID
     * 
     * @param authHeader Authorization头值
     * @return 用户ID
     * @throws ResponseStatusException 401 如果token无效或过期
     */
    protected Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少或无效的Authorization头");
        }
        
        String token = authHeader.substring(7); // 移除"Bearer "
        
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token已过期");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token格式错误");
        } catch (io.jsonwebtoken.SignatureException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token签名无效");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token验证失败");
        }
    }
    
    /**
     * 验证资源访问权限
     * 
     * @param resourceOwnerId 资源所有者ID
     * @param currentUserId 当前用户ID
     * @throws ResponseStatusException 403 如果无权访问
     */
    protected void checkPermission(Long resourceOwnerId, Long currentUserId) {
        if (!resourceOwnerId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问此资源");
        }
    }
}
