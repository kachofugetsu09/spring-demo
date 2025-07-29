package site.hnfy258.kafkademo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.service.RebalanceDemoConsumerService;
import site.hnfy258.service.RebalanceObserverService;

import java.util.HashMap;
import java.util.Map;

/**
 * 再平衡演示控制器
 * 提供API来观察和控制再平衡过程
 */
@RestController
@RequestMapping("/api/rebalance")
@Slf4j
public class RebalanceController {
    
    @Autowired
    private RebalanceDemoConsumerService rebalanceDemoConsumerService;
    
    @Autowired
    private RebalanceObserverService rebalanceObserverService;
    
    /**
     * 获取再平衡统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getRebalanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("consumerStats", rebalanceDemoConsumerService.getConsumerStats());
        stats.put("rebalanceCount", rebalanceObserverService.getRebalanceCount());
        stats.put("consumerInstanceId", rebalanceDemoConsumerService.getConsumerInstanceId());
        stats.put("timestamp", System.currentTimeMillis());
        
        log.info("📊 再平衡统计查询: {}", stats);
        return stats;
    }
    
    /**
     * 手动触发状态日志输出
     */
    @PostMapping("/log-status")
    public Map<String, String> logCurrentStatus() {
        rebalanceObserverService.logCurrentStatus();
        log.info("🔍 手动触发状态日志输出");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "状态已输出到日志");
        response.put("consumerInstanceId", rebalanceDemoConsumerService.getConsumerInstanceId());
        return response;
    }
    
    /**
     * 获取再平衡观察指南
     */
    @GetMapping("/guide")
    public Map<String, Object> getRebalanceGuide() {
        Map<String, Object> guide = new HashMap<>();
        guide.put("title", "Kafka再平衡观察指南");
        guide.put("steps", new String[]{
            "1. 启动当前应用实例",
            "2. 观察日志中的初始分区分配",
            "3. 启动第二个应用实例 (使用不同端口: java -jar app.jar --server.port=8081)",
            "4. 观察再平衡过程和分区重新分配",
            "5. 停止其中一个实例，观察再次再平衡",
            "6. 使用 /api/rebalance/stats 查看统计信息"
        });
        guide.put("currentInstance", rebalanceDemoConsumerService.getConsumerInstanceId());
        guide.put("topicInfo", "user_behavior_logs topic 有3个分区");
        guide.put("consumerGroup", "rebalance_demo_group");
        
        return guide;
    }
}