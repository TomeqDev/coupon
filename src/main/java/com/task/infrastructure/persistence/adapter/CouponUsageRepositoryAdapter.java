package com.task.infrastructure.persistence.adapter;

import com.task.domain.model.CouponUsage;
import com.task.domain.repository.CouponUsageRepository;
import com.task.infrastructure.persistence.mapper.CouponUsageMapper;
import com.task.infrastructure.persistence.repository.JpaCouponUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CouponUsageRepositoryAdapter implements CouponUsageRepository {

    private final JpaCouponUsageRepository jpaCouponUsageRepository;
    private final CouponUsageMapper couponUsageMapper;

    @Override
    public CouponUsage save(CouponUsage couponUsage) {
        var entity = couponUsageMapper.toEntity(couponUsage);
        var saved = jpaCouponUsageRepository.save(entity);
        return couponUsageMapper.toDomain(saved);
    }

    @Override
    public boolean existsByCouponIdAndUserId(UUID couponId, String userId) {
        return jpaCouponUsageRepository.existsByCouponIdAndUserId(couponId, userId);
    }
}
