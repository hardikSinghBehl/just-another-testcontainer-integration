package com.behl.receptacle.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.behl.receptacle.TestContainerExtension;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(TestContainerExtension.class)
public class CacheServiceIT {
  
    @Autowired
    private CacheService cacheService;
    
    @Test
    @SneakyThrows
    void shouldStoreRecordInCacheSuccessfullyForConfiguredTtl() {
        // Prepare test data
        final var key = RandomString.make(10);
        final var userRecord = User.create();
        final var timeToLive = Duration.ofSeconds(2);
        
        // Store the user record in the cache with the specified key and time-to-live
        cacheService.set(key, userRecord, timeToLive);
        
        // Verify that the cached record is retrieved successfully and contains the expected values
        var retrievedRecord = cacheService.fetch(key, User.class);
        assertThat(retrievedRecord.isPresent()).isTrue();
        assertThat(retrievedRecord.get().getId()).isEqualTo(userRecord.getId());
        assertThat(retrievedRecord.get().getFirstName()).isEqualTo(userRecord.getFirstName());
        assertThat(retrievedRecord.get().getLastName()).isEqualTo(userRecord.getLastName());
        
        // Verify that the cached record is no longer available after the time-to-live duration has expired
        Thread.sleep(timeToLive.toMillis());
        retrievedRecord = cacheService.fetch(key, User.class);
        assertThat(retrievedRecord.isEmpty()).isTrue();
    }
    
    @Test
    void shouldUpdateCachedRecordValue() {
        // Prepare test data
        final var key = RandomString.make(10);
        final var initialUserRecord = User.create();
        final var updatedUserRecord = User.create();
        final var timeToLive = Duration.ofSeconds(10);
        
        // Store the user record in the cache with the specified key and time-to-live
        cacheService.set(key, initialUserRecord, timeToLive);
        
        // Verify that the cached record is retrieved successfully and contains the expected values
        var retrievedRecord = cacheService.fetch(key, User.class);
        assertThat(retrievedRecord.isPresent()).isTrue();
        assertThat(retrievedRecord.get().getId()).isEqualTo(initialUserRecord.getId());
        assertThat(retrievedRecord.get().getFirstName()).isEqualTo(initialUserRecord.getFirstName());
        assertThat(retrievedRecord.get().getLastName()).isEqualTo(initialUserRecord.getLastName());
        
        // Update the cached record with the updated User record
        cacheService.update(key, updatedUserRecord);
        
        // Verify that the cached record is updated and contains the values of the updated User record
        retrievedRecord = cacheService.fetch(key, User.class);
        assertThat(retrievedRecord.isPresent()).isTrue();
        assertThat(retrievedRecord.get().getId()).isEqualTo(updatedUserRecord.getId());
        assertThat(retrievedRecord.get().getFirstName()).isEqualTo(updatedUserRecord.getFirstName());
        assertThat(retrievedRecord.get().getLastName()).isEqualTo(updatedUserRecord.getLastName());
    }
    
    @Test
    void shouldFetchCachedRecordSuccessfully() {
        // Prepare test data and save generated record in cache
        final var key = RandomString.make(10);
        final var userRecord = User.create();
        final var timeToLive = Duration.ofSeconds(10);
        cacheService.set(key, userRecord, timeToLive);
        
        // Fetch the user record with the key used to save
        var retrievedRecord = cacheService.fetch(key, User.class);
        
        // Verify that the record corresponding to the key exists in the cache and contains the expected values
        assertThat(retrievedRecord.isPresent()).isTrue();
        assertThat(retrievedRecord.get().getId()).isEqualTo(userRecord.getId());
        assertThat(retrievedRecord.get().getFirstName()).isEqualTo(userRecord.getFirstName());
        assertThat(retrievedRecord.get().getLastName()).isEqualTo(userRecord.getLastName());
    }
    
    @Test
    void shouldReturnEmptyValueIfKeyNotPresentInCache() {
        // Generate a random key that is not present in the cache
        final var key = RandomString.make(10);
        
        // Fetch the value associated with the key from the cache
        var retrievedRecord = cacheService.fetch(key, String.class);
        
        // Verify that the retrieved value is empty
        assertThat(retrievedRecord.isEmpty()).isTrue();
    }
    
    @Test
    void shouldDeleteCachedRecordSuccessfullyAgainstPresentKey() {
        // Prepare test data and save generated record in cache
        final var key = RandomString.make(10);
        final var userRecord = User.create();
        final var timeToLive = Duration.ofSeconds(1000);
        cacheService.set(key, userRecord, timeToLive);
  
        // Verify that the cached record is present before deletion
        var retrievedRecord = cacheService.fetch(key, User.class);
        assertThat(retrievedRecord.isPresent()).isTrue();
        
        // Delete the cached record associated with the key
        cacheService.delete(key);
        
        // Verify that the cached record is no longer present after deletion
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
