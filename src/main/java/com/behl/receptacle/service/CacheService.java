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

    /**
     * Sets the specified key-value pair in the cache with the given time-to-live duration.
     *
     * @param key        The key to be cached.
     * @param value      The value to be cached.
     * @param timeToLive The duration for which the value should be cached.
     */
    public void set(@NonNull final String key, @NonNull final Object value, @NonNull final Duration timeToLive) {
        redisTemplate.opsForValue().set(key, value, timeToLive);
        log.info("Cached value with key '{}' for {} seconds", key, timeToLive.toSeconds());
    }
    
    /**
     * Updates the value of the existing cached entry corresponding to the specified key.
     *
     * @param key   The key of the cached entry to be updated.
     * @param value The new value to be set for the cached entry.
     */
    public void update(@NonNull final String key, @NonNull final Object value) {
        redisTemplate.opsForValue().setIfPresent(key, value);
        log.info("Updated cached value with key '{}'", key);
    }

    /**
     * Fetches the value associated with the specified key from the cache and converts it to the
     * target class.
     *
     * @param key The key of the cached entry to be fetched.
     * @param targetClass The class to which the fetched value should be converted.
     * @param <T> The type of the target class.
     * @return An Optional containing the fetched value of type T, or an empty Optional if no value
     *         is found in the cache for the given key.
     */
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
    
    /**
     * Deletes the cached entry with the specified key from the cache.
     *
     * @param key The key of the cached entry to be deleted.
     */
    public void delete(@NonNull final String key) {
        final var deletionStatus = redisTemplate.delete(key);
        log.info("Deleted cached value with key '{}' {}", key, deletionStatus ? "Successfully" : "Unsuccessfully");
    }

}
