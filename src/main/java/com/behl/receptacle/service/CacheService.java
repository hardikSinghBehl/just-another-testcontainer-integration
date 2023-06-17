package com.behl.receptacle.service;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void set(@NonNull final String key, @NonNull final Object value, @NonNull final Duration timeToLive) {
        redisTemplate.opsForValue().set(key, value, timeToLive);
    }
    
    public void update(@NonNull final String key, @NonNull final Object value) {
        redisTemplate.opsForValue().setIfPresent(key, value);
    }

    public <T> Optional<T> fetch(@NonNull final String key, @NonNull final Class<T> destinationClass) {
        final var value = Optional.ofNullable(redisTemplate.opsForValue().get(key));
        if (value.isEmpty()) {
            return Optional.empty();          
        }
        T result = objectMapper.convertValue(value.get(), destinationClass);
        return Optional.of(result);
    }
    
    public void delete(@NonNull final String key) {
        redisTemplate.delete(key);
    }

}
