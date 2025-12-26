package com.mindrevol.backend.modules.system.repository;
import com.mindrevol.backend.modules.system.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppConfigRepository extends JpaRepository<AppConfig, String> {
    Optional<AppConfig> findByKey(String key);
}