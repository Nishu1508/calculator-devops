package com.spe.calculator.exception;

import com.spe.calculator.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Converts exceptions thrown anywhere in the controllers into a consistent JSON error body
 * with HTTP 400 Bad Request.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** b = 0 on /divide. */
    @ExceptionHandler(DivisionByZeroException.class)
    public ResponseEntity<ErrorResponse> handleDivisionByZero(
            DivisionByZeroException ex, HttpServletRequest request) {
        return badRequest(ex.getMessage(), request);
    }

    /** Parameter missing entirely (?a=1) or present but empty (?a=1&b=). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return badRequest(
                "Required parameter '" + ex.getParameterName() + "' is missing or empty", request);
    }

    /** Parameter is not a valid number (?a=abc&b=2). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return badRequest(
                "Parameter '" + ex.getName() + "' must be a valid number but was '" + ex.getValue() + "'",
                request);
    }

    private ResponseEntity<ErrorResponse> badRequest(String message, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        log.warn("400 Bad Request on {}: {}", request.getRequestURI(), message);
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(
                        status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
