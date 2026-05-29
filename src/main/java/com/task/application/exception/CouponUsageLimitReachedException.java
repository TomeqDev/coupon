package com.task.application.exception;

public class CouponUsageLimitReachedException extends RuntimeException {

    public CouponUsageLimitReachedException(String code) {
        super("Coupon '" + code + "' has reached its maximum usage limit");
    }
}
