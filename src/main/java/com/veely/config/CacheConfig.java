package com.veely.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "countries",
            "locations", 
            "suppliers",
            "taskTypes",
            "employeeRoles",
            "vehicleDetails",
            "employeeDetails",
            "documentTypes"
        ));
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }
    
    @Bean
    public Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(500)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .recordStats(); // Abilita statistiche per monitoring
    }
    
    // Cache specifica per dati che cambiano raramente
    @Bean
    public CacheManager staticDataCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(Arrays.asList("staticData"));
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(24, TimeUnit.HOURS) // Cache per 24 ore
            .recordStats());
        return cacheManager;
    }
}

