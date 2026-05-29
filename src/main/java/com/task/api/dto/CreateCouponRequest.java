package com.task.api.dto;

import com.task.api.validation.ValidCountryCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(
        @NotBlank @Size(max = 50) String code,
        @Min(1) int maxUsages,
        @NotBlank @ValidCountryCode String country
) {
}
