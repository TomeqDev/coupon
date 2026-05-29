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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterCouponUsageUseCaseTest {

    private static final String CODE = "SUMMER2024";
    private static final String USER_ID = "user-123";
    private static final String IP_ADDRESS = "185.0.0.1";
    private static final String COUNTRY = "PL";
    private static final int MAX_USAGES = 100;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private GeoLocationService geoLocationService;

    private RegisterCouponUsageUseCase registerCouponUsageUseCase;

    @BeforeEach
    void setUp() {
        registerCouponUsageUseCase = new RegisterCouponUsageUseCase(
                couponRepository, couponUsageRepository, geoLocationService, new SimpleMeterRegistry());
    }

    private Coupon givenCoupon(int currentUsages) {
        return new Coupon(UUID.randomUUID(), CODE, null, MAX_USAGES, currentUsages, COUNTRY);
    }

    private void givenCouponExists(Coupon coupon) {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(coupon));
    }

    private void givenCountryResolved(String country) {
        when(geoLocationService.getCountryCode(IP_ADDRESS)).thenReturn(country);
    }

    private void givenUserHasNotUsedCoupon(Coupon coupon) {
        when(couponUsageRepository.existsByCouponIdAndUserId(coupon.getId(), USER_ID)).thenReturn(false);
    }

    private void givenIncrementSucceeds(Coupon coupon) {
        when(couponRepository.incrementUsage(coupon.getId())).thenReturn(1);
        when(couponUsageRepository.save(any(CouponUsage.class))).thenAnswer(invocation -> {
            CouponUsage usage = invocation.getArgument(0);
            return new CouponUsage(UUID.randomUUID(), usage.couponId(), usage.userId(), usage.usedAt());
        });
    }

    @Nested
    @DisplayName("Successful usage registration")
    class SuccessfulUsage {

        @Test
        @DisplayName("should register usage and return usage record")
        void shouldRegisterUsageSuccessfully() {
            Coupon coupon = givenCoupon(5);
            givenCouponExists(coupon);
            givenCountryResolved(COUNTRY);
            givenUserHasNotUsedCoupon(coupon);
            givenIncrementSucceeds(coupon);

            CouponUsage result = registerCouponUsageUseCase.execute(CODE, USER_ID, IP_ADDRESS);

            assertThat(result.id()).isNotNull();
            assertThat(result.couponId()).isEqualTo(coupon.getId());
            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.usedAt()).isNotNull();

            verify(couponRepository).incrementUsage(coupon.getId());
            verify(couponUsageRepository).save(any(CouponUsage.class));
        }

        @Test
        @DisplayName("should handle case-insensitive coupon code")
        void shouldHandleCaseInsensitiveCode() {
            Coupon coupon = givenCoupon(5);
            givenCouponExists(coupon);
            givenCountryResolved(COUNTRY);
            givenUserHasNotUsedCoupon(coupon);
            givenIncrementSucceeds(coupon);

            CouponUsage result = registerCouponUsageUseCase.execute(CODE.toLowerCase(), USER_ID, IP_ADDRESS);

            assertThat(result).isNotNull();
            verify(couponRepository).findByCode(CODE);
        }
    }

    @Nested
    @DisplayName("Failed usage registration")
    class FailedUsage {

        @Test
        @DisplayName("should throw 404 when coupon not found")
        void shouldThrowWhenCouponNotFound() {
            when(couponRepository.findByCode(CODE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> registerCouponUsageUseCase.execute(CODE, USER_ID, IP_ADDRESS))
                    .isInstanceOf(CouponNotFoundException.class)
                    .hasMessageContaining(CODE);

            verify(geoLocationService, never()).getCountryCode(any());
            verify(couponUsageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw 403 when country not allowed")
        void shouldThrowWhenCountryNotAllowed() {
            Coupon coupon = givenCoupon(5);
            givenCouponExists(coupon);
            givenCountryResolved("US");

            assertThatThrownBy(() -> registerCouponUsageUseCase.execute(CODE, USER_ID, IP_ADDRESS))
                    .isInstanceOf(CountryNotAllowedException.class)
                    .hasMessageContaining(COUNTRY)
                    .hasMessageContaining("US");

            verify(couponRepository, never()).incrementUsage(any());
            verify(couponUsageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw 409 when user already used coupon")
        void shouldThrowWhenUserAlreadyUsedCoupon() {
            Coupon coupon = givenCoupon(5);
            givenCouponExists(coupon);
            givenCountryResolved(COUNTRY);
            when(couponUsageRepository.existsByCouponIdAndUserId(coupon.getId(), USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> registerCouponUsageUseCase.execute(CODE, USER_ID, IP_ADDRESS))
                    .isInstanceOf(CouponAlreadyUsedByUserException.class)
                    .hasMessageContaining(USER_ID)
                    .hasMessageContaining(CODE);

            verify(couponRepository, never()).incrementUsage(any());
            verify(couponUsageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw 409 when usage limit reached")
        void shouldThrowWhenUsageLimitReached() {
            Coupon coupon = givenCoupon(MAX_USAGES);
            givenCouponExists(coupon);
            givenCountryResolved(COUNTRY);
            givenUserHasNotUsedCoupon(coupon);
            when(couponRepository.incrementUsage(coupon.getId())).thenReturn(0);

            assertThatThrownBy(() -> registerCouponUsageUseCase.execute(CODE, USER_ID, IP_ADDRESS))
                    .isInstanceOf(CouponUsageLimitReachedException.class)
                    .hasMessageContaining(CODE);

            verify(couponUsageRepository, never()).save(any());
        }
    }
}
