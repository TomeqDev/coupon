package com.task.application;

import com.task.application.exception.CouponAlreadyUsedByUserException;
import com.task.application.exception.CouponNotFoundException;
import com.task.application.exception.CouponUsageLimitReachedException;
import com.task.application.exception.CountryNotAllowedException;
import com.task.application.port.GeoLocationService;
import com.task.domain.model.Coupon;
import com.task.domain.model.CouponUsage;
import com.task.domain.repository.CouponRepository;
import com.task.domain.repository.CouponUsageRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RegisterCouponUsageUseCase {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final GeoLocationService geoLocationService;
    private final Counter usageSuccessCounter;
    private final Counter usageFailedCounter;

    public RegisterCouponUsageUseCase(CouponRepository couponRepository,
                                      CouponUsageRepository couponUsageRepository,
                                      GeoLocationService geoLocationService,
                                      MeterRegistry meterRegistry) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.geoLocationService = geoLocationService;
        this.usageSuccessCounter = buildCounter("coupon.usage.success",
                "Number of successful coupon usages", meterRegistry);
        this.usageFailedCounter = buildCounter("coupon.usage.failed",
                "Number of failed coupon usage attempts", meterRegistry);
    }

    @Transactional
    public CouponUsage execute(String code, String userId, String ipAddress) {
        log.info("Registering usage for coupon='{}', userId='{}', ip='{}'", code, userId, ipAddress);

        String normalizedCode = code.toUpperCase();
        Coupon coupon = findCouponOrThrow(normalizedCode);

        validateCountry(coupon, ipAddress);
        validateNotAlreadyUsed(coupon, userId, normalizedCode);
        applyUsageIncrement(coupon, normalizedCode);

        CouponUsage saved = recordUsage(coupon, userId);

        log.info("Coupon usage registered: coupon='{}', userId='{}', usageId='{}'",
                normalizedCode, userId, saved.id());
        usageSuccessCounter.increment();

        return saved;
    }

    private Coupon findCouponOrThrow(String code) {
        return couponRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.warn("Coupon not found: code='{}'", code);
                    usageFailedCounter.increment();
                    return new CouponNotFoundException(code);
                });
    }

    private void validateCountry(Coupon coupon, String ipAddress) {
        String userCountry = geoLocationService.getCountryCode(ipAddress);
        if (!coupon.isCountryAllowed(userCountry)) {
            log.warn("Country not allowed: coupon='{}', couponCountry='{}', userCountry='{}'",
                    coupon.getCode(), coupon.getCountry(), userCountry);
            usageFailedCounter.increment();
            throw new CountryNotAllowedException(coupon.getCountry(), userCountry);
        }
    }

    private void validateNotAlreadyUsed(Coupon coupon, String userId, String code) {
        if (couponUsageRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
            log.warn("Duplicate usage attempt: coupon='{}', userId='{}'", code, userId);
            usageFailedCounter.increment();
            throw new CouponAlreadyUsedByUserException(code, userId);
        }
    }

    private void applyUsageIncrement(Coupon coupon, String code) {
        int updated = couponRepository.incrementUsage(coupon.getId());
        if (updated == 0) {
            log.warn("Usage limit reached: coupon='{}'", code);
            usageFailedCounter.increment();
            throw new CouponUsageLimitReachedException(code);
        }
    }

    private CouponUsage recordUsage(Coupon coupon, String userId) {
        CouponUsage usage = CouponUsage.create(coupon.getId(), userId);
        return couponUsageRepository.save(usage);
    }

    private static Counter buildCounter(String name, String description, MeterRegistry registry) {
        return Counter.builder(name).description(description).register(registry);
    }
}
