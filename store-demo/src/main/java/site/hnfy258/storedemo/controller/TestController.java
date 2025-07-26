package site.hnfy258.storedemo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.storedemo.security.CustomUserDetails;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    /**
     * 公开接口，无需认证
     */
    @GetMapping("/public")
    public ResponseEntity<?> publicEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "这是一个公开接口，无需认证");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 需要认证的接口
     */
    @GetMapping("/protected")
    public ResponseEntity<?> protectedEndpoint(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "这是一个受保护的接口，需要认证");
        response.put("user", userDetails.getUsername());
        response.put("roles", userDetails.getRoles());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 需要ADMIN角色的接口
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminEndpoint(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "这是一个管理员接口，需要ADMIN角色");
        response.put("user", userDetails.getUsername());
        response.put("roles", userDetails.getRoles());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 需要MANAGER角色的接口
     */
    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> managerEndpoint(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "这是一个管理者接口，需要MANAGER角色");
        response.put("user", userDetails.getUsername());
        response.put("roles", userDetails.getRoles());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 需要USER角色的接口
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> userEndpoint(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "这是一个普通用户接口，需要USER角色");
        response.put("user", userDetails.getUsername());
        response.put("roles", userDetails.getRoles());
        return ResponseEntity.ok(response);
    }
}