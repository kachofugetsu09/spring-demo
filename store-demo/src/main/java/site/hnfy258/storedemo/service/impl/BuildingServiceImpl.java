package site.hnfy258.storedemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.hnfy258.storedemo.entity.Building;
import site.hnfy258.storedemo.mapper.BuildingMapper;
import site.hnfy258.storedemo.service.BuildingService;
import site.hnfy258.storedemo.util.CacheUtil;

import java.time.Duration;

@Slf4j
@Service
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements BuildingService {
    
    @Autowired
    private CacheUtil cacheUtil;
    
    private static final String CACHE_KEY_PREFIX = "building:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    @Transactional
    public Building getById(String id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // 使用通用缓存工具类，简洁的实现
        return cacheUtil.executeWithCache(
            cacheKey,
            () -> super.getById(id), // 数据库查询逻辑
            Building.class,
            CACHE_TTL
        );
    }

    @Override
    public boolean save(Building building) {
        boolean result = super.save(building);
        if (result && building.getId() != null) {
            // 保存成功后，更新缓存
            String cacheKey = CACHE_KEY_PREFIX + building.getId();
            cacheUtil.cacheData(cacheKey, building, CACHE_TTL);
        }
        return result;
    }
    
    @Override
    public boolean updateById(Building building) {
        boolean result = super.updateById(building);
        if (result) {
            // 更新成功后，删除缓存（Write Around策略）
            String cacheKey = CACHE_KEY_PREFIX + building.getId();
            cacheUtil.evictCache(cacheKey);
            log.info("Deleted cache for building id: {} after update", building.getId());
        }
        return result;
    }
    
    @Override
    public boolean removeById(String id) {
        boolean result = super.removeById(id);
        if (result) {
            // 删除成功后，删除缓存
            String cacheKey = CACHE_KEY_PREFIX + id;
            cacheUtil.evictCache(cacheKey);
            log.info("Deleted cache for building id: {} after removal", id);
        }
        return result;
    }
}
