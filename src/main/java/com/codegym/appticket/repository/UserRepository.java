package com.codegym.appticket.repository;

import com.codegym.appticket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    java.util.Optional<com.codegym.appticket.entity.User> findByEmail(String email);
}
