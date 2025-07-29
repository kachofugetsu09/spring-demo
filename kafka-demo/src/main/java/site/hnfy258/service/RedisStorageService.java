package site.hnfy258.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.UserBehavior;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisStorageService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    
    // Redis key前缀
    private static final String USER_BEHAVIOR_PREFIX = "user_behavior:";
    private static final String USER_BEHAVIOR_LIST_PREFIX = "user_behavior_list:";
    private static final String DAILY_STATS_PREFIX = "daily_stats:";
    private static final String MESSAGE_ID_SET = "processed_message_ids";
    
    public RedisStorageService(RedisTemplate<String, Object> redisTemplate, 
                              StringRedisTemplate stringRedisTemplate,
                              ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 存储用户行为数据
     */
    public void storeUserBehavior(UserBehavior userBehavior) {
        try {
            String messageId = userBehavior.getMessageId();
            
            // 1. 检查消息是否已处理（幂等性）
            if (isMessageProcessed(messageId)) {
                log.warn("Message {} already processed, skipping storage", messageId);
                return;
            }
            
            // 2. 存储单个用户行为记录
            String behaviorKey = USER_BEHAVIOR_PREFIX + messageId;
            redisTemplate.opsForValue().set(behaviorKey, userBehavior, Duration.ofDays(7));
            
            // 3. 添加到用户行为列表（按用户ID分组）
            String userListKey = USER_BEHAVIOR_LIST_PREFIX + userBehavior.getUserId();
            redisTemplate.opsForList().leftPush(userListKey, userBehavior);
            redisTemplate.expire(userListKey, Duration.ofDays(7));
            
            // 4. 更新每日统计
            updateDailyStats(userBehavior);
            
            // 5. 标记消息已处理
            markMessageAsProcessed(messageId);
            
            log.debug("Stored user behavior: userId={}, actionType={}, messageId={}", 
                     userBehavior.getUserId(), userBehavior.getActionType(), messageId);
                     
        } catch (Exception e) {
            log.error("Error storing user behavior to Redis: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查消息是否已处理
     */
    public boolean isMessageProcessed(String messageId) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(MESSAGE_ID_SET, messageId));
    }
    
    /**
     * 标记消息已处理
     */
    private void markMessageAsProcessed(String messageId) {
        stringRedisTemplate.opsForSet().add(MESSAGE_ID_SET, messageId);
        // 设置过期时间，避免集合无限增长
        stringRedisTemplate.expire(MESSAGE_ID_SET, Duration.ofDays(7));
    }
    
    /**
     * 更新每日统计
     */
    private void updateDailyStats(UserBehavior userBehavior) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String statsKey = DAILY_STATS_PREFIX + today;
        
        // 总数统计
        redisTemplate.opsForHash().increment(statsKey, "total_count", 1);
        
        // 按动作类型统计
        redisTemplate.opsForHash().increment(statsKey, "action:" + userBehavior.getActionType(), 1);
        
        // 按用户统计
        redisTemplate.opsForHash().increment(statsKey, "user:" + userBehavior.getUserId(), 1);
        
        // 设置过期时间
        redisTemplate.expire(statsKey, Duration.ofDays(30));
    }
    
    /**
     * 获取用户行为列表
     */
    public List<Object> getUserBehaviorList(String userId, int limit) {
        String userListKey = USER_BEHAVIOR_LIST_PREFIX + userId;
        return redisTemplate.opsForList().range(userListKey, 0, limit - 1);
    }
    
    /**
     * 获取每日统计
     */
    public Object getDailyStats(String date) {
        String statsKey = DAILY_STATS_PREFIX + date;
        return redisTemplate.opsForHash().entries(statsKey);
    }
    
    /**
     * 获取今日统计
     */
    public Object getTodayStats() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return getDailyStats(today);
    }
    
    /**
     * 获取处理的消息总数
     */
    public Long getProcessedMessageCount() {
        return stringRedisTemplate.opsForSet().size(MESSAGE_ID_SET);
    }
    
    /**
     * 清理过期数据
     */
    public void cleanupExpiredData() {
        try {
            // 获取所有用户行为key
            Set<String> behaviorKeys = redisTemplate.keys(USER_BEHAVIOR_PREFIX + "*");
            if (behaviorKeys != null && !behaviorKeys.isEmpty()) {
                log.info("Found {} user behavior keys in Redis", behaviorKeys.size());
            }
            
            // 获取所有用户列表key
            Set<String> listKeys = redisTemplate.keys(USER_BEHAVIOR_LIST_PREFIX + "*");
            if (listKeys != null && !listKeys.isEmpty()) {
                log.info("Found {} user behavior list keys in Redis", listKeys.size());
            }
            
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage(), e);
        }
    }
}