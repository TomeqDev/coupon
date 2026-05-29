package com.task.domain.repository;

import com.task.domain.model.Coupon;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findByCode(String code);

    int incrementUsage(UUID couponId);
}
