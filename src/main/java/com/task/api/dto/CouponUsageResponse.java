package com.task.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CouponUsageResponse(
        UUID id,
        String couponCode,
        String userId,
        LocalDateTime usedAt
) {
}
