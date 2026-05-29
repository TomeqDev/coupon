package com.task.application;

import com.task.application.exception.CouponAlreadyExistsException;
import com.task.domain.model.Coupon;
import com.task.domain.repository.CouponRepository;
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
class CreateCouponUseCaseTest {

    private static final String CODE = "SUMMER2024";
    private static final int MAX_USAGES = 100;
    private static final String COUNTRY = "PL";

    @Mock
    private CouponRepository couponRepository;

    private CreateCouponUseCase createCouponUseCase;

    @BeforeEach
    void setUp() {
        createCouponUseCase = new CreateCouponUseCase(couponRepository, new SimpleMeterRegistry());
    }

    @Nested
    @DisplayName("Successful creation")
    class SuccessfulCreation {

        @BeforeEach
        void setUp() {
            when(couponRepository.findByCode(any())).thenReturn(Optional.empty());
            when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> {
                Coupon coupon = invocation.getArgument(0);
                return new Coupon(UUID.randomUUID(), coupon.getCode(), coupon.getCreationDate(),
                        coupon.getMaxUsages(), coupon.getCurrentUsages(), coupon.getCountry());
            });
        }

        @Test
        @DisplayName("should create coupon with all fields populated")
        void shouldCreateCouponSuccessfully() {
            Coupon result = createCouponUseCase.execute(CODE, MAX_USAGES, COUNTRY);

            assertThat(result.getId()).isNotNull();
            assertThat(result.getCode()).isEqualTo(CODE);
            assertThat(result.getMaxUsages()).isEqualTo(MAX_USAGES);
            assertThat(result.getCurrentUsages()).isZero();
            assertThat(result.getCountry()).isEqualTo(COUNTRY);
            assertThat(result.getCreationDate()).isNotNull();

            verify(couponRepository).save(any(Coupon.class));
        }

        @Test
        @DisplayName("should convert code to uppercase")
        void shouldConvertCodeToUpperCase() {
            Coupon result = createCouponUseCase.execute("summer2024", 10, "US");

            assertThat(result.getCode()).isEqualTo(CODE);
        }

        @Test
        @DisplayName("should convert country to uppercase")
        void shouldConvertCountryToUpperCase() {
            Coupon result = createCouponUseCase.execute("TEST", 10, "pl");

            assertThat(result.getCountry()).isEqualTo("PL");
        }
    }

    @Nested
    @DisplayName("Failed creation")
    class FailedCreation {

        @Test
        @DisplayName("should throw exception when code already exists")
        void shouldThrowExceptionWhenCodeAlreadyExists() {
            Coupon existing = Coupon.create(CODE, 50, COUNTRY);
            when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> createCouponUseCase.execute(CODE, MAX_USAGES, COUNTRY))
                    .isInstanceOf(CouponAlreadyExistsException.class)
                    .hasMessageContaining(CODE);

            verify(couponRepository, never()).save(any());
        }
    }
}
