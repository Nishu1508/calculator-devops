package com.spe.calculator.dto;

import java.math.BigDecimal;

/**
 * Successful response body, e.g. {"operation":"add","a":2,"b":3,"result":5}.
 *
 * <p>A Java record is immutable and Jackson serialises its components as JSON fields.
 */
public record CalculationResponse(String operation, BigDecimal a, BigDecimal b, BigDecimal result) {}
