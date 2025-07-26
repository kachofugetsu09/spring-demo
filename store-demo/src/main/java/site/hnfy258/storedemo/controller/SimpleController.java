package site.hnfy258.storedemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SimpleController {
    
    /**
     * 最简单的GET接口
     */
    @GetMapping("/simple")
    public Map<String, Object> simple() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "简单接口正常工作");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    /**
     * 简单的POST接口，模拟注册
     */
    @PostMapping("/simple-register")
    public Map<String, Object> simpleRegister(@RequestParam String username,
                                             @RequestParam String password,
                                             @RequestParam String phone) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "简单注册接口正常工作");
        response.put("username", username);
        response.put("phone", phone);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}