package com.spe.calculator.dto;

import java.time.Instant;

/**
 * Structured error body returned for every 4xx produced by the API, e.g.
 * {"timestamp":"...","status":400,"error":"Bad Request","message":"...","path":"/api/v1/divide"}.
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }
}
