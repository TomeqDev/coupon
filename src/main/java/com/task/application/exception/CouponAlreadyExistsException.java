package com.task.application.exception;

public class CouponAlreadyExistsException extends RuntimeException {

    public CouponAlreadyExistsException(String code) {
        super("Coupon with code '" + code + "' already exists");
    }
}
