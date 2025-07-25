package site.hnfy258.storedemo.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 通用缓存工具类 - 使用 Redisson 分布式锁解决缓存三剑客问题
 * 1. 缓存穿透：缓存空值
 * 2. 缓存击穿：Redisson 分布式锁 + Watchdog 自动续期
 * 3. 缓存雪崩：随机过期时间
 */
@Slf4j
@Component
public class CacheUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String NULL_VALUE = "NULL";
    private static final Duration NULL_CACHE_TTL = Duration.ofMinutes(5);
    private static final int MAX_RETRY_TIMES = 3;
    private static final long RETRY_INTERVAL_MS = 100;
    private final Random random = new Random();
    /**
     * 通用缓存查询模板方法
     * 
     * @param cacheKey     缓存key
     * @param dataLoader   数据加载器（数据库查询逻辑）
     * @param clazz        返回值类型
     * @param ttl          缓存过期时间
     * @param <T>          泛型类型
     * @return 查询结果
     */
    public <T> T executeWithCache(String cacheKey, Supplier<T> dataLoader, Class<T> clazz, Duration ttl) {
        // 1. 尝试从缓存获取数据
        T cachedData = getFromCache(cacheKey, clazz);
        if (cachedData != null || isNullValue(cacheKey)) {
            return cachedData;
        }

        // 2. 缓存未命中，使用分布式锁防止缓存击穿
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 3. 尝试获取锁，最多等待5秒，锁持有时间30秒（Watchdog会自动续期）
            boolean lockAcquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            
            if (lockAcquired) {
                try {
                    log.info("Acquired distributed lock for key: {}", cacheKey);
                    
                    // 4. 双重检查：再次尝试从缓存获取（可能其他线程已经加载了数据）
                    cachedData = getFromCache(cacheKey, clazz);
                    if (cachedData != null || isNullValue(cacheKey)) {
                        return cachedData;
                    }

                    // 5. 执行数据加载逻辑
                    T data = dataLoader.get();
                    
                    // 6. 将结果写入缓存
                    if (data != null) {
                        cacheDataWithRandomTTL(cacheKey, data, ttl);
                    } else {
                        cacheNullValue(cacheKey);
                    }
                    
                    return data;
                } finally {
                    // 7. 释放锁（Redisson会自动处理锁的释放和Watchdog停止）
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.info("Released distributed lock for key: {}", cacheKey);
                    }
                }
            } else {
                // 8. 获取锁失败，等待并重试
                log.warn("Failed to acquire lock for key: {}, fallback to retry", cacheKey);
                return retryGetFromCache(cacheKey, dataLoader, clazz, ttl, 0);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for lock: {}", cacheKey, e);
            // 降级处理：直接查询数据库
            return dataLoader.get();
        } catch (Exception e) {
            log.error("Error occurred while processing cache: {}", cacheKey, e);
            return dataLoader.get();
        }
    }

    /**
     * 重试获取缓存数据
     */
    private <T> T retryGetFromCache(String cacheKey, Supplier<T> dataLoader, Class<T> clazz, Duration ttl, int retryCount) {
        if (retryCount >= MAX_RETRY_TIMES) {
            log.warn("Max retry times reached for key: {}, fallback to database query", cacheKey);
            return dataLoader.get();
        }

        try {
            Thread.sleep(RETRY_INTERVAL_MS);
            
            // 重试时先检查缓存
            T cachedData = getFromCache(cacheKey, clazz);
            if (cachedData != null || isNullValue(cacheKey)) {
                return cachedData;
            }

            // 递归重试
            return retryGetFromCache(cacheKey, dataLoader, clazz, ttl, retryCount + 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return dataLoader.get();
        }
    }

    /**
     * 从缓存获取数据
     */
    private <T> T getFromCache(String cacheKey, Class<T> clazz) {
        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null && !NULL_VALUE.equals(cachedValue)) {
                log.info("Cache hit for key: {}", cacheKey);
                return objectMapper.readValue(cachedValue, clazz);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached data for key: {}", cacheKey, e);
            // 删除损坏的缓存
            redisTemplate.delete(cacheKey);
        }
        return null;
    }

    /**
     * 检查是否为空值缓存
     */
    private boolean isNullValue(String cacheKey) {
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (NULL_VALUE.equals(cachedValue)) {
            log.info("Cache hit null value for key: {}", cacheKey);
            return true;
        }
        return false;
    }
    /**
     * 缓存数据，使用随机过期时间防止缓存雪崩
     */
    public <T> void cacheData(String cacheKey, T data, Duration baseTTL) {
        cacheDataWithRandomTTL(cacheKey, data, baseTTL);
    }

    /**
     * 缓存数据，使用随机过期时间防止缓存雪崩
     */
    private <T> void cacheDataWithRandomTTL(String cacheKey, T data, Duration baseTTL) {
        try {
            String jsonValue = objectMapper.writeValueAsString(data);
            // 基础TTL + 随机时间（0-20%），防止缓存雪崩
            long randomSeconds = (long) (baseTTL.getSeconds() * 0.2 * random.nextDouble());
            Duration randomTTL = baseTTL.plusSeconds(randomSeconds);
            
            redisTemplate.opsForValue().set(cacheKey, jsonValue, randomTTL);
            log.info("Cached data for key: {} with TTL: {} seconds", cacheKey, randomTTL.getSeconds());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize data for cache key: {}", cacheKey, e);
        }
    }

    /**
     * 缓存空值，防止缓存穿透
     */
    private void cacheNullValue(String cacheKey) {
        // 空值缓存时间较短，加少量随机时间
        long randomSeconds = random.nextInt(60); // 0-59秒
        Duration randomNullTTL = NULL_CACHE_TTL.plusSeconds(randomSeconds);
        
        redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, randomNullTTL);
        log.info("Cached null value for key: {} with TTL: {} seconds", cacheKey, randomNullTTL.getSeconds());
    }

    /**
     * 删除缓存
     */
    public void evictCache(String cacheKey) {
        redisTemplate.delete(cacheKey);
        log.info("Evicted cache for key: {}", cacheKey);
    }

    /**
     * 批量删除缓存
     */
    public void evictCaches(String... cacheKeys) {
        if (cacheKeys != null && cacheKeys.length > 0) {
            redisTemplate.delete(java.util.Arrays.asList(cacheKeys));
            log.info("Evicted caches for keys: {}", java.util.Arrays.toString(cacheKeys));
        }
    }
}

