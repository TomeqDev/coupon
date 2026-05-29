package com.task.domain.repository;

import com.task.domain.model.CouponUsage;
import java.util.UUID;

public interface CouponUsageRepository {

    CouponUsage save(CouponUsage couponUsage);

    boolean existsByCouponIdAndUserId(UUID couponId, String userId);
}
