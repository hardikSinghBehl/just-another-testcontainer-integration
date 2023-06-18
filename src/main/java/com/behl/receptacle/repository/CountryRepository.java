package com.behl.receptacle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.behl.receptacle.entity.Country;

@Repository
public interface CountryRepository extends JpaRepository<Country, Integer> {
}