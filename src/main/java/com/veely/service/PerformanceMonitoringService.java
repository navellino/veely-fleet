package com.veely.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceMonitoringService {
    
    private final CacheManager cacheManager;
    
    public Map<String, CacheStatistics> getCacheStatistics() {
        Map<String, CacheStatistics> stats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
            if (springCache instanceof CaffeineCache) {
                Cache<Object, Object> caffeineCache = ((CaffeineCache) springCache).getNativeCache();
                CacheStats cacheStats = caffeineCache.stats();
                
                stats.put(cacheName, new CacheStatistics(
                    caffeineCache.estimatedSize(),
                    cacheStats.hitCount(),
                    cacheStats.missCount(),
                    cacheStats.hitRate(),
                    cacheStats.evictionCount()
                ));
            }
        });
        
        return stats;
    }
    
    public record CacheStatistics(
        long size,
        long hitCount,
        long missCount,
        double hitRate,
        long evictionCount
    ) {}
    
    public void logCacheStatistics() {
        log.info("=== Cache Statistics ===");
        getCacheStatistics().forEach((name, stats) -> {
            log.info("Cache {}: size={}, hits={}, misses={}, hitRate={:.2f}%, evictions={}",
                name, stats.size(), stats.hitCount(), stats.missCount(), 
                stats.hitRate() * 100, stats.evictionCount());
        });
    }
}
