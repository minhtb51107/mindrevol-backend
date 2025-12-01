package com.mindrevol.backend.modules.store.repository;

import com.mindrevol.backend.modules.store.entity.StoreItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreItemRepository extends JpaRepository<StoreItem, Long> {
    List<StoreItem> findByIsActiveTrue();
    Optional<StoreItem> findByCode(String code);
}