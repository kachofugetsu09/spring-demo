package site.hnfy258.storedemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.hnfy258.storedemo.entity.Building;
import site.hnfy258.storedemo.mapper.BuildingMapper;
import site.hnfy258.storedemo.service.BuildingService;

import java.time.Duration;

@Slf4j
@Service
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements BuildingService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String CACHE_KEY_PREFIX = "building:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    
    @Override
    public Building getById(String id) {
        // Cache Aside 模式 - 查询缓存
        String cacheKey = CACHE_KEY_PREFIX + id;
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedValue != null) {
            log.info("Cache hit for building id: {}", id);
            try {
                return objectMapper.readValue(cachedValue, Building.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize cached building", e);
                // 缓存反序列化失败，删除缓存
                redisTemplate.delete(cacheKey);
            }
        }
        
        // 缓存未命中，查询数据库
        log.info("Cache miss for building id: {}, querying database", id);
        Building building = super.getById(id);
        
        if (building != null) {
            // 写入缓存
            try {
                String jsonValue = objectMapper.writeValueAsString(building);
                redisTemplate.opsForValue().set(cacheKey, jsonValue, CACHE_TTL);
                log.info("Cached building id: {}", id);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize building for cache", e);
            }
        }
        
        return building;
    }

    @Override
    public boolean save(Building building) {
        boolean result = super.save(building);
        if (result && building.getId() != null) {
            // 保存成功后，更新缓存
            updateCache(building);
        }
        return result;
    }
    
    @Override
    public boolean updateById(Building building) {
        boolean result = super.updateById(building);
        if (result) {
            // 更新成功后，删除缓存（Write Around策略）或更新缓存
            String cacheKey = CACHE_KEY_PREFIX + building.getId();
            redisTemplate.delete(cacheKey);
            log.info("Deleted cache for building id: {} after update", building.getId());
            
            // 也可以选择直接更新缓存（Write Through策略）
            // updateCache(building);
        }
        return result;
    }
    
    @Override
    public boolean removeById(String id) {
        boolean result = super.removeById(id);
        if (result) {
            // 删除成功后，删除缓存
            String cacheKey = CACHE_KEY_PREFIX + id;
            redisTemplate.delete(cacheKey);
            log.info("Deleted cache for building id: {} after removal", id);
        }
        return result;
    }
    
    private void updateCache(Building building) {
        try {
            String cacheKey = CACHE_KEY_PREFIX + building.getId();
            String jsonValue = objectMapper.writeValueAsString(building);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, CACHE_TTL);
            log.info("Updated cache for building id: {}", building.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to update cache for building", e);
        }
    }
}
