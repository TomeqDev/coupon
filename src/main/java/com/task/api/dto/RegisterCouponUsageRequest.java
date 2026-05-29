package com.task.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterCouponUsageRequest(
        @NotBlank String userId
) {
}
