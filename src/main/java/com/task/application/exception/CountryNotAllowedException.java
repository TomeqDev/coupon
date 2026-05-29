package com.task.application.exception;

public class CountryNotAllowedException extends RuntimeException {

    public CountryNotAllowedException(String couponCountry, String userCountry) {
        super("Coupon is restricted to country '" + couponCountry + "', but request is from '" + userCountry + "'");
    }
}
