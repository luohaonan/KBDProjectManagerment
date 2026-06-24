package com.kbd.pms.service;

import com.kbd.pms.entity.User;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全上下文工具，用于从当前JWT Token中提取当前登录用户信息
 * 通过 SecurityContextHolder 获取已认证的用户，防止客户端伪造userId
 */
@Component
public class SecurityHelper {

    private final UserRepository userRepository;

    public SecurityHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 获取当前登录用户ID（对应 iam_user.id）
     */
    public long getCurrentUserId() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new ApiException(401, "未找到当前用户信息，请重新登录"));
    }

    /**
     * 获取当前登录用户名
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(401, "未登录或登录已过期");
        }
        return authentication.getName();
    }
}