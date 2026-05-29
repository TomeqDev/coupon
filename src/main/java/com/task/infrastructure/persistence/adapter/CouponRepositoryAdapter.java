package com.task.infrastructure.persistence.adapter;

import com.task.domain.model.Coupon;
import com.task.domain.repository.CouponRepository;
import com.task.infrastructure.persistence.mapper.CouponMapper;
import com.task.infrastructure.persistence.repository.JpaCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryAdapter implements CouponRepository {

    private final JpaCouponRepository jpaCouponRepository;
    private final CouponMapper couponMapper;

    @Override
    public Coupon save(Coupon coupon) {
        var entity = couponMapper.toEntity(coupon);
        var saved = jpaCouponRepository.save(entity);
        return couponMapper.toDomain(saved);
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return jpaCouponRepository.findByCode(code.toUpperCase())
                .map(couponMapper::toDomain);
    }

    @Override
    public int incrementUsage(UUID couponId) {
        return jpaCouponRepository.incrementUsage(couponId);
    }
}
