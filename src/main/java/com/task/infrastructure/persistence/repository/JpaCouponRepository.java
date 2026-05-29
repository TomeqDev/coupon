package com.task.infrastructure.persistence.repository;

import com.task.infrastructure.persistence.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface JpaCouponRepository extends JpaRepository<CouponEntity, UUID> {

    Optional<CouponEntity> findByCode(String code);

    @Modifying
    @Query("UPDATE CouponEntity c SET c.currentUsages = c.currentUsages + 1 " +
            "WHERE c.id = :id AND c.currentUsages < c.maxUsages")
    int incrementUsage(@Param("id") UUID id);
}
