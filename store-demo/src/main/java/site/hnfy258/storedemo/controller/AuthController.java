package site.hnfy258.storedemo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.storedemo.constants.DeviceType;
import site.hnfy258.storedemo.dto.JwtTokenDto;
import site.hnfy258.storedemo.dto.LoginRequest;
import site.hnfy258.storedemo.dto.RefreshTokenRequest;
import site.hnfy258.storedemo.security.CustomUserDetails;
import site.hnfy258.storedemo.service.AuthService;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtTokenDto tokenDto = authService.login(loginRequest);
            return ResponseEntity.ok(tokenDto);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            JwtTokenDto tokenDto = authService.refreshToken(request);
            return ResponseEntity.ok(tokenDto);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @RequestParam DeviceType deviceType) {
        try {
            authService.logout(userDetails.getUserId(), deviceType);
            Map<String, String> response = new HashMap<>();
            response.put("message", "登出成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username,
                                     @RequestParam String password,
                                     @RequestParam String phone) {
        try {
            System.out.println("🔍 注册请求到达: username=" + username + ", phone=" + phone);
            authService.register(username, password, phone);
            Map<String, String> response = new HashMap<>();
            response.put("message", "注册成功");
            System.out.println("✅ 注册成功: " + username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ 注册失败: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "未认证用户");
            return ResponseEntity.status(401).body(error);
        }
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", userDetails.getUserId());
        userInfo.put("username", userDetails.getUsername());
        userInfo.put("roles", userDetails.getRoles());
        return ResponseEntity.ok(userInfo);
    }
}