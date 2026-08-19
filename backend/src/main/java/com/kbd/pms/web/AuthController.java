package com.kbd.pms.web;

import com.kbd.pms.entity.User;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.UserRepository;
import com.kbd.pms.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public static final String INITIAL_PASSWORD = "123456";

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                          PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
        boolean mustChangePassword = passwordEncoder.matches(INITIAL_PASSWORD, user.getPassword());

        return ResponseEntity.ok(Map.of("token", jwt, "mustChangePassword", mustChangePassword));
    }

    @PostMapping("/change-initial-password")
    public ResponseEntity<?> changeInitialPassword(Authentication authentication,
                                                    @RequestBody ChangePasswordRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(401, "登录已失效，请重新登录");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
        if (!passwordEncoder.matches(INITIAL_PASSWORD, user.getPassword())) {
            throw new ApiException(400, "当前账号不需要修改初始密码");
        }
        if (request.getCurrentPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ApiException(400, "当前密码不正确");
        }

        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 64) {
            throw new ApiException(400, "新密码长度必须为 6-64 位");
        }
        if (!newPassword.equals(request.getConfirmPassword())) {
            throw new ApiException(400, "两次输入的新密码不一致");
        }
        if (INITIAL_PASSWORD.equals(newPassword)) {
            throw new ApiException(400, "新密码不能与初始密码相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "密码修改成功，请重新登录"));
    }

    // 临时接口：生成 BCrypt 密码哈希
    @PostMapping("/encode")
    public ResponseEntity<?> encodePassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "password is required"));
        }
        String encoded = passwordEncoder.encode(password);
        return ResponseEntity.ok(Map.of("password", password, "encoded", encoded));
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}