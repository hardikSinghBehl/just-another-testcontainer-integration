package com.behl.receptacle.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.behl.receptacle.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
}