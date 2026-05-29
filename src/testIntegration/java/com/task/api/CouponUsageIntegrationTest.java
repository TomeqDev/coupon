package com.task.api;

import com.task.BaseIntegrationTest;
import com.task.api.dto.CreateCouponRequest;
import com.task.api.dto.RegisterCouponUsageRequest;
import com.task.application.port.GeoLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/coupons/{code}/usages")
class CouponUsageIntegrationTest extends BaseIntegrationTest {

    private static final String COUPON_COUNTRY = "PL";

    @MockBean
    private GeoLocationService geoLocationService;

    private void givenUserLocatedIn(String country) {
        when(geoLocationService.getCountryCode(anyString())).thenReturn(country);
    }

    private void createCoupon(String code, int maxUsages, String country) throws Exception {
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCouponRequest(code, maxUsages, country))))
                .andExpect(status().isCreated());
    }

    private void performUsage(String code, String userId) throws Exception {
        mockMvc.perform(post("/api/coupons/{code}/usages", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterCouponUsageRequest(userId))))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("Successful usage")
    class SuccessfulUsage {

        @BeforeEach
        void setUp() {
            givenUserLocatedIn(COUPON_COUNTRY);
        }

        @Test
        @DisplayName("should register usage and return 200 with usage details")
        void shouldRegisterUsageSuccessfully() throws Exception {
            createCoupon("USAGE01", 10, COUPON_COUNTRY);

            mockMvc.perform(post("/api/coupons/USAGE01/usages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterCouponUsageRequest("user-1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.couponCode").value("USAGE01"))
                    .andExpect(jsonPath("$.userId").value("user-1"))
                    .andExpect(jsonPath("$.usedAt").isNotEmpty());
        }

        @Test
        @DisplayName("should handle case-insensitive coupon code in URL")
        void shouldHandleCaseInsensitiveCouponCode() throws Exception {
            createCoupon("USAGE05", 10, COUPON_COUNTRY);

            mockMvc.perform(post("/api/coupons/usage05/usages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterCouponUsageRequest("user-1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("user-1"));
        }
    }

    @Nested
    @DisplayName("Coupon not found")
    class CouponNotFound {

        @Test
        @DisplayName("should return 404 when coupon does not exist")
        void shouldReturn404WhenCouponNotFound() throws Exception {
            mockMvc.perform(post("/api/coupons/NONEXISTENT/usages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterCouponUsageRequest("user-1"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("Country restriction")
    class CountryRestriction {

        @Test
        @DisplayName("should return 403 when user IP resolves to different country")
        void shouldReturn403WhenCountryNotAllowed() throws Exception {
            createCoupon("USAGE02", 10, COUPON_COUNTRY);
            givenUserLocatedIn("US");

            mockMvc.perform(post("/api/coupons/USAGE02/usages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterCouponUsageRequest("user-1"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
    }

    @Nested
    @DisplayName("Conflict - usage constraints")
    class UsageConflicts {

        @BeforeEach
        void setUp() {
            givenUserLocatedIn(COUPON_COUNTRY);
        }

        @Test
        @DisplayName("should return 409 when same user tries to use coupon twice")
        void shouldReturn409WhenUserAlreadyUsedCoupon() throws Exception {
            createCoupon("USAGE03", 10, COUPON_COUNTRY);
            performUsage("USAGE03", "user-1");

            mockMvc.perform(post("/api/coupons/USAGE03/usages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterCouponUsageRequest("user-1"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("User 'user-1' has already used coupon 'USAGE03'"));
        }

        @Test
        @DisplayName("should return 409 when coupon max usage limit is reached")
        void shouldReturn409WhenUsageLimitReached() throws Exception {
            createCoupon("USAGE04", 1, COUPON_COUNTRY);
            performUsage("USAGE04", "user-1");

            mockMvc.perform(post("/api/coupons/USAGE04/usages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterCouponUsageRequest("user-2"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("Validation errors")
    class ValidationErrors {

        @Test
        @DisplayName("should return 400 when userId is blank")
        void shouldReturn400WhenUserIdIsBlank() throws Exception {
            mockMvc.perform(post("/api/coupons/ANYCODE/usages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterCouponUsageRequest(""))))
                    .andExpect(status().isBadRequest());
        }
    }
}
