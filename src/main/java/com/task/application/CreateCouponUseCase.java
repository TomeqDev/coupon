package com.task.application;

import com.task.domain.model.Coupon;
import com.task.domain.repository.CouponRepository;
import com.task.application.exception.CouponAlreadyExistsException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CreateCouponUseCase {

    private final CouponRepository couponRepository;
    private final Counter couponCreatedCounter;
    private final Counter couponCreationFailedCounter;

    public CreateCouponUseCase(CouponRepository couponRepository, MeterRegistry meterRegistry) {
        this.couponRepository = couponRepository;
        this.couponCreatedCounter = Counter.builder("coupon.created")
                .description("Number of coupons successfully created")
                .register(meterRegistry);
        this.couponCreationFailedCounter = Counter.builder("coupon.creation.failed")
                .description("Number of failed coupon creation attempts")
                .register(meterRegistry);
    }

    @Transactional
    public Coupon execute(String code, int maxUsages, String country) {
        log.info("Creating coupon with code='{}', maxUsages={}, country='{}'", code, maxUsages, country);

        checkCouponExists(code);
        Coupon saved = storeCoupon(code, maxUsages, country);

        log.info("Coupon created successfully: id={}, code='{}'", saved.getId(), saved.getCode());
        couponCreatedCounter.increment();

        return saved;
    }

    private Coupon storeCoupon(String code, int maxUsages, String country) {
        Coupon coupon = Coupon.create(code, maxUsages, country);
        return couponRepository.save(coupon);
    }

    private void checkCouponExists(String code) {
        couponRepository.findByCode(code.toUpperCase())
                .ifPresent(existing -> {
                    log.warn("Coupon creation failed - code '{}' already exists", code);
                    couponCreationFailedCounter.increment();
                    throw new CouponAlreadyExistsException(code);
                });
    }
}
