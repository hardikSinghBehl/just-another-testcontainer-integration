package com.behl.receptacle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import com.behl.receptacle.entity.User;
import jakarta.persistence.EntityNotFoundException;
import net.bytebuddy.utility.RandomString;

@SpringBootTest
@ActiveProfiles("test")
public class PersistenceServiceIT {

    @Autowired
    private PersistenceService persistenceService;

    private static MySQLContainer<?> mySQLContainer;

    static { 
        mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8"));
        mySQLContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
    }

    @Test
    void shouldReturnNonEmptyCountryListPostFlywayMigrationExecution() {
        // Retrieve the list of countries saved in the datasource
        final var countries = persistenceService.getAllCountries();
        
        // Verify that the list of countries is not empty and flyway migration script is executed
        assertThat(countries).isNotEmpty();
    }

    @Test
    void shouldSaveUserRecordInDatabaseAndReturnValidUserId() {
        // Prepare test data
        final var firstName = RandomString.make(10);
        final var lastName = RandomString.make(10);
        final var country = persistenceService.getAllCountries().stream().findAny().orElse(null);
        final var user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCountry(country);

        // Save the user record in datasource
        final var savedUserId = persistenceService.saveUser(user);

        // Fetch the user from the datasource using the assigned user ID and validate data integrity
        final var fetchedUser = persistenceService.getUserById(savedUserId);
        assertThat(fetchedUser.getId()).isEqualTo(savedUserId);
        assertThat(fetchedUser.getFirstName()).isEqualTo(firstName);
        assertThat(fetchedUser.getLastName()).isEqualTo(lastName);
        assertThat(fetchedUser.getCountry().getId()).isEqualTo(country.getId());        
     }
    
    @Test
    void shouldDeleteSavedUserRecordInDatabase() {
        // Prepare test data
        final var country = persistenceService.getAllCountries().stream().findAny().orElse(null);
        final var user = new User();
        user.setFirstName(RandomString.make(10));
        user.setLastName(RandomString.make(10));
        user.setCountry(country);
        
        // Save the user record in datasource
        final var savedUserId = persistenceService.saveUser(user);

        // Fetch the user from the datasource using the assigned user ID and validate correctness
        final var fetchedUser = persistenceService.getUserById(savedUserId);
        assertThat(fetchedUser.getId()).isEqualTo(savedUserId); 
        
        // Delete the user record from the datasource
        persistenceService.deleteUser(savedUserId);
        
        // Verify that attempting to fetch the deleted user results throws EntityNotFoundException
        assertThrows(EntityNotFoundException.class, () -> persistenceService.getUserById(savedUserId));
     }

}