package com.spe.calculator.exception;

/** Thrown when the divisor is zero; mapped to HTTP 400 by {@link GlobalExceptionHandler}. */
public class DivisionByZeroException extends RuntimeException {

    public DivisionByZeroException() {
        super("Division by zero is not allowed: parameter 'b' must not be 0");
    }
}
