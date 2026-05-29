package com.task.application.exception;

public class CouponAlreadyUsedByUserException extends RuntimeException {

    public CouponAlreadyUsedByUserException(String code, String userId) {
        super("User '" + userId + "' has already used coupon '" + code + "'");
    }
}
