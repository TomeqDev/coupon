package com.task.api.dto;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String traceId,
        LocalDateTime timestamp,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(int status, String error, String message, @Nullable Tracer tracer) {
        return new ErrorResponse(status, error, message, resolveTraceId(tracer), LocalDateTime.now(), null);
    }

    public static ErrorResponse withFieldErrors(int status, String error, String message,
                                                 List<FieldError> fieldErrors, @Nullable Tracer tracer) {
        return new ErrorResponse(status, error, message, resolveTraceId(tracer), LocalDateTime.now(), fieldErrors);
    }

    private static String resolveTraceId(@Nullable Tracer tracer) {
        if (tracer == null) return null;
        Span span = tracer.currentSpan();
        return span != null ? span.context().traceId() : null;
    }
}
