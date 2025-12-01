package com.mindrevol.backend.modules.user.repository;

import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByHandle(String handle);
    boolean existsByEmail(String email);
    boolean existsByHandle(String handle);
}