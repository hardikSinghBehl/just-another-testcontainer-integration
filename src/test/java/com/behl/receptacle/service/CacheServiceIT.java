package com.behl.receptacle.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
public class CacheServiceIT {
  
    @Autowired
    private CacheService cacheService;
  
    private static GenericContainer<?> redisContainer;
    private static int redisPort = 6379;
    private static String redisPassword = RandomString.make(10);
  
    static {
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0.11-alpine3.18"))
            .withExposedPorts(redisPort).withCommand("redis-server", "--requirepass", redisPassword);
        redisContainer.start();
    }
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(redisContainer.getMappedPort(redisPort)));
        registry.add("spring.data.redis.password", () -> redisPassword);
    }
    
    @Test
    @SneakyThrows
    void shouldStoreRecordInCacheSuccessfullyForConfiguredTtl() {
        final var key = RandomString.make(10);
        final var userRecord = User.create();
        final var timeToLive = Duration.ofSeconds(2);
        
        cacheService.set(key, userRecord, timeToLive);
        
        var retrievedRecord = cacheService.fetch(key, User.class);
        assertThat(retrievedRecord.isPresent()).isTrue();
        assertThat(retrievedRecord.get().getId()).isEqualTo(userRecord.getId());
        assertThat(retrievedRecord.get().getFirstName()).isEqualTo(userRecord.getFirstName());
        assertThat(retrievedRecord.get().getLastName()).isEqualTo(userRecord.getLastName());
        
        Thread.sleep(timeToLive.toMillis());
        retrievedRecord = cacheService.fetch(key, User.class);
        assertThat(retrievedRecord.isEmpty()).isTrue();
    }
    
    @Test
    void shouldUpdateCachedRecordValue() {
        final var key = RandomString.make(10);
        final var initialUserRecord = User.create();
        final var updatedUserRecord = User.create();
        final var timeToLive = Duration.ofSeconds(10);
        
        cacheService.set(key, initialUserRecord, timeToLive);
        
        var retrievedRecord = cacheService.fetch(key, User.class);
        assertThat(retrievedRecord.isPresent()).isTrue();
        assertThat(retrievedRecord.get().getId()).isEqualTo(initialUserRecord.getId());
        assertThat(retrievedRecord.get().getFirstName()).isEqualTo(initialUserRecord.getFirstName());
        assertThat(retrievedRecord.get().getLastName()).isEqualTo(initialUserRecord.getLastName());
        
        cacheService.update(key, updatedUserRecord);
        
        retrievedRecord = cacheService.fetch(key, User.class);
        assertThat(retrievedRecord.isPresent()).isTrue();
        assertThat(retrievedRecord.get().getId()).isEqualTo(updatedUserRecord.getId());
        assertThat(retrievedRecord.get().getFirstName()).isEqualTo(updatedUserRecord.getFirstName());
        assertThat(retrievedRecord.get().getLastName()).isEqualTo(updatedUserRecord.getLastName());
    }
    
    @Test
    void shouldFetchCachedRecordSuccessfully() {
        final var key = RandomString.make(10);
        final var userRecord = User.create();
        final var timeToLive = Duration.ofSeconds(10);
        cacheService.set(key, userRecord, timeToLive);
        
        var retrievedRecord = cacheService.fetch(key, User.class);
        
        assertThat(retrievedRecord.isPresent()).isTrue();
        assertThat(retrievedRecord.get().getId()).isEqualTo(userRecord.getId());
        assertThat(retrievedRecord.get().getFirstName()).isEqualTo(userRecord.getFirstName());
        assertThat(retrievedRecord.get().getLastName()).isEqualTo(userRecord.getLastName());
    }
    
    @Test
    void shouldReturnEmptyValueIfKeyNotPresentInCache() {
        final var key = RandomString.make(10);
        
        var retrievedRecord = cacheService.fetch(key, String.class);
        
        assertThat(retrievedRecord.isEmpty()).isTrue();
    }
    
    @Test
    void shouldDeleteCachedRecordSuccessfullyAgainstPresentKey() {
        final var key = RandomString.make(10);
        final var userRecord = User.create();
        final var timeToLive = Duration.ofSeconds(1000);
        cacheService.set(key, userRecord, timeToLive);
  
        var retrievedRecord = cacheService.fetch(key, User.class);
        
        assertThat(retrievedRecord.isPresent()).isTrue();
        
        cacheService.delete(key);
        
        retrievedRecord = cacheService.fetch(key, User.class);
        assertThat(retrievedRecord.isEmpty()).isTrue();
    }
    
    @Getter
    @Setter
    static class User {
      
        private String id;
        private String firstName;
        private String lastName;
        
        private User() {
            this.id = RandomString.make(5);
            this.firstName = RandomString.make(5);
            this.lastName = RandomString.make(5);
        }
        
        public static User create() {
            return new User();
        }
      
    }

}
