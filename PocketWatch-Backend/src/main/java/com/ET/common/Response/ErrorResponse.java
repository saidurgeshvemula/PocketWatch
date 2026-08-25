package com.ET.common.Response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        String code,
        String message,
        List<String> errors,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(false, code, message, null, LocalDateTime.now());
    }

    public static ErrorResponse ofValidation(List<String> errors) {
        return new ErrorResponse(false, "VALIDATION_ERROR", "Request validation failed", errors, LocalDateTime.now());
    }
}
