package site.hnfy258.kafkademo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.service.RedisStorageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/redis")
@Slf4j
public class RedisController {

    @Autowired
    private RedisStorageService redisStorageService;

    @GetMapping("/stats/today")
    public Map<String, Object> getTodayStats() {
        Map<String, Object> response = new HashMap<>();
        try {
            Object stats = redisStorageService.getTodayStats();
            Long processedCount = redisStorageService.getProcessedMessageCount();
            
            response.put("status", "success");
            response.put("todayStats", stats);
            response.put("totalProcessedMessages", processedCount);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get today's stats: " + e.getMessage());
            log.error("Failed to get today's stats", e);
        }
        return response;
    }

    @GetMapping("/stats/{date}")
    public Map<String, Object> getStatsByDate(@PathVariable String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            Object stats = redisStorageService.getDailyStats(date);
            response.put("status", "success");
            response.put("date", date);
            response.put("stats", stats);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get stats for date " + date + ": " + e.getMessage());
            log.error("Failed to get stats for date {}", date, e);
        }
        return response;
    }

    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserBehaviors(@PathVariable String userId, 
                                               @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Object> behaviors = redisStorageService.getUserBehaviorList(userId, limit);
            response.put("status", "success");
            response.put("userId", userId);
            response.put("behaviors", behaviors);
            response.put("count", behaviors.size());
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to get user behaviors: " + e.getMessage());
            log.error("Failed to get user behaviors for userId {}", userId, e);
        }
        return response;
    }

    @PostMapping("/cleanup")
    public Map<String, Object> cleanupExpiredData() {
        Map<String, Object> response = new HashMap<>();
        try {
            redisStorageService.cleanupExpiredData();
            response.put("status", "success");
            response.put("message", "Cleanup completed");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Cleanup failed: " + e.getMessage());
            log.error("Cleanup failed", e);
        }
        return response;
    }

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long processedCount = redisStorageService.getProcessedMessageCount();
            response.put("status", "success");
            response.put("message", "Redis storage service is healthy");
            response.put("totalProcessedMessages", processedCount);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Redis storage service is unhealthy: " + e.getMessage());
            log.error("Redis health check failed", e);
        }
        return response;
    }
}