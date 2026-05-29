package com.task.api;

import com.task.BaseIntegrationTest;
import com.task.api.dto.CreateCouponRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/coupons")
class CouponControllerIntegrationTest extends BaseIntegrationTest {

    private void performCreateCoupon(CreateCouponRequest request) throws Exception {
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Nested
    @DisplayName("Successful creation")
    class SuccessfulCreation {

        @Test
        @DisplayName("should return 201 with Location header and coupon body")
        void shouldCreateCouponAndReturn201WithLocation() throws Exception {
            mockMvc.perform(post("/api/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateCouponRequest("INTTEST01", 50, "PL"))))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.code").value("INTTEST01"))
                    .andExpect(jsonPath("$.maxUsages").value(50))
                    .andExpect(jsonPath("$.currentUsages").value(0))
                    .andExpect(jsonPath("$.country").value("PL"))
                    .andExpect(jsonPath("$.creationDate").isNotEmpty());
        }

        @Test
        @DisplayName("should normalize code and country to uppercase")
        void shouldCreateCouponWithCaseInsensitiveCode() throws Exception {
            mockMvc.perform(post("/api/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateCouponRequest("lowercase1", 10, "us"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("LOWERCASE1"))
                    .andExpect(jsonPath("$.country").value("US"));
        }
    }

    @Nested
    @DisplayName("Conflict - duplicate code")
    class DuplicateCode {

        @Test
        @DisplayName("should return 409 when coupon code already exists")
        void shouldReturn409WhenCouponCodeAlreadyExists() throws Exception {
            performCreateCoupon(new CreateCouponRequest("DUPLICATE1", 10, "US"));

            mockMvc.perform(post("/api/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateCouponRequest("DUPLICATE1", 10, "US"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Coupon with code 'DUPLICATE1' already exists"));
        }
    }

    @Nested
    @DisplayName("Validation errors")
    class ValidationErrors {

        @Test
        @DisplayName("should return 400 when code is blank")
        void shouldReturn400WhenCodeIsBlank() throws Exception {
            mockMvc.perform(post("/api/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateCouponRequest("", 10, "PL"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));
        }

        @Test
        @DisplayName("should return 400 when maxUsages is zero")
        void shouldReturn400WhenMaxUsagesIsZero() throws Exception {
            mockMvc.perform(post("/api/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateCouponRequest("VALID01", 0, "PL"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").isArray());
        }

        @Test
        @DisplayName("should return 400 when country is not a valid ISO code")
        void shouldReturn400WhenCountryIsInvalid() throws Exception {
            mockMvc.perform(post("/api/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateCouponRequest("VALID02", 10, "XX"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("country"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("must be a valid ISO 3166-1 alpha-2 country code"));
        }
    }
}
