package com.notification.service.exception;

import java.util.Map;

public record ErrorResponse(
        String error,
        Map<String, String> fields
) {
    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, null);
    }
}