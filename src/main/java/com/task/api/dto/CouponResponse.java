package com.task.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        String code,
        LocalDateTime creationDate,
        int maxUsages,
        int currentUsages,
        String country
) {
}
