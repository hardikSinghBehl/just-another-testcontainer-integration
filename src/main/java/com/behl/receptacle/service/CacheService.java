package com.behl.receptacle.service;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void set(@NonNull final String key, @NonNull final Object value, @NonNull final Duration timeToLive) {
        redisTemplate.opsForValue().set(key, value, timeToLive);
        log.info("Cached value with key '{}' for {} seconds", key, timeToLive.toSeconds());
    }
    
    public void update(@NonNull final String key, @NonNull final Object value) {
        redisTemplate.opsForValue().setIfPresent(key, value);
        log.info("Updated cached value with key '{}'", key);
    }

    public <T> Optional<T> fetch(@NonNull final String key, @NonNull final Class<T> targetClass) {
        final var value = Optional.ofNullable(redisTemplate.opsForValue().get(key));
        if (value.isEmpty()) {
            log.info("No cached value found for key '{}'", key);
            return Optional.empty();          
        }
        T result = objectMapper.convertValue(value.get(), targetClass);
        log.info("Fetched cached value with key '{}'", key);
        return Optional.of(result);
    }
    
    public void delete(@NonNull final String key) {
        redisTemplate.delete(key);
        log.info("Deleted cached value with key '{}'", key);
    }

}
