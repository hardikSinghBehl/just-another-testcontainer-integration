package com.behl.receptacle.service;

import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.behl.receptacle.entity.Country;
import com.behl.receptacle.entity.User;
import com.behl.receptacle.repository.CountryRepository;
import com.behl.receptacle.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistenceService {

    private final CountryRepository countryRepository;
    private final UserRepository userRepository;

    public List<Country> getAllCountries() {
        log.info("Fetching list of all countries");
        final var countries = countryRepository.findAll();
        log.info("Successfully fetched {} country records from the datasource", countries.size());
        return countries;
    }

    public UUID saveUser(@NonNull final User user) {
        final var savedUser = userRepository.save(user);
        log.info("User record with ID '{}' saved successfully", savedUser.getId());
        return savedUser.getId();
    }

    @Transactional(readOnly = true)
    public User getUserById(@NonNull final UUID userId) {
        log.info("Fetching user by ID '{}'", userId);
        final var user = userRepository.getReferenceById(userId);
        Hibernate.initialize(user.getCountry());
        log.info("User record with ID '{}' fetched successfully", userId);
        return user;
    }

    public void deleteUser(@NonNull final UUID userId) {
        log.info("Deleting user by ID '{}'", userId);
        userRepository.deleteById(userId);
        log.info("User record with ID '{}' deleted successfully", userId);
    }

}