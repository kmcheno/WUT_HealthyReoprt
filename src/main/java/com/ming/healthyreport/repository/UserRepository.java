package com.ming.healthyreport.repository;

import com.ming.healthyreport.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
