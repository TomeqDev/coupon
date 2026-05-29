package com.task.infrastructure.persistence.repository;

import com.task.infrastructure.persistence.entity.CouponUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaCouponUsageRepository extends JpaRepository<CouponUsageEntity, UUID> {

    boolean existsByCouponIdAndUserId(UUID couponId, String userId);
}
