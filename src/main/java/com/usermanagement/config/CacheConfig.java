package com.usermanagement.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // Uses Spring Boot's default ConcurrentMapCacheManager
    // For production, consider Redis or Caffeine
}
