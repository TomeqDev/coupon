package com.task.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class Coupon {

    private UUID id;
    private String code;
    private LocalDateTime creationDate;
    private int maxUsages;
    private int currentUsages;
    private String country;

    public static Coupon create(String code, int maxUsages, String country) {
        return new Coupon(null, code.toUpperCase(), LocalDateTime.now(), maxUsages, 0, country.toUpperCase());
    }

    public boolean isUsageLimitReached() {
        return currentUsages >= maxUsages;
    }

    public boolean isCountryAllowed(String requestCountry) {
        return this.country.equalsIgnoreCase(requestCountry);
    }

    public void incrementUsage() {
        if (isUsageLimitReached()) {
            throw new IllegalStateException("Coupon usage limit reached");
        }
        this.currentUsages++;
    }
}
