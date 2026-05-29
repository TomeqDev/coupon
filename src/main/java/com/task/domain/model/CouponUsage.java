package com.task.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record CouponUsage(UUID id, UUID couponId, String userId, LocalDateTime usedAt) {

    public static CouponUsage create(UUID couponId, String userId) {
        return new CouponUsage(null, couponId, userId, LocalDateTime.now());
    }
}
