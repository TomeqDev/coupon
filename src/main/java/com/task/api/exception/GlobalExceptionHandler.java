package com.task.api.exception;

import com.task.api.dto.ErrorResponse;
import com.task.application.exception.CouponAlreadyExistsException;
import com.task.application.exception.CouponAlreadyUsedByUserException;
import com.task.application.exception.CouponNotFoundException;
import com.task.application.exception.CouponUsageLimitReachedException;
import com.task.application.exception.CountryNotAllowedException;
import com.task.application.exception.GeoLocationException;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Tracer tracer;

    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCouponNotFound(CouponNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage(), tracer));
    }

    @ExceptionHandler(CountryNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleCountryNotAllowed(CountryNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage(), tracer));
    }

    @ExceptionHandler(CouponAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCouponAlreadyExists(CouponAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), tracer));
    }

    @ExceptionHandler(CouponAlreadyUsedByUserException.class)
    public ResponseEntity<ErrorResponse> handleCouponAlreadyUsedByUser(CouponAlreadyUsedByUserException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), tracer));
    }

    @ExceptionHandler(CouponUsageLimitReachedException.class)
    public ResponseEntity<ErrorResponse> handleCouponUsageLimitReached(CouponUsageLimitReachedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), tracer));
    }

    @ExceptionHandler(GeoLocationException.class)
    public ResponseEntity<ErrorResponse> handleGeoLocationError(GeoLocationException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(503, "Service Unavailable", ex.getMessage(), tracer));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.withFieldErrors(400, "Bad Request", "Validation failed", fieldErrors, tracer));
    }
}
