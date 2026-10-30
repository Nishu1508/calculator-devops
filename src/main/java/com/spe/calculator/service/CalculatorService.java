package com.spe.calculator.service;

import com.spe.calculator.exception.DivisionByZeroException;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Service;

/**
 * Pure business logic for the calculator.
 *
 * <p>BigDecimal is used instead of double so results are exact (0.1 + 0.2 = 0.3, not
 * 0.30000000000000004) and so whole numbers are returned as whole numbers (2 + 3 = 5, not 5.0).
 */
@Service
public class CalculatorService {

    /** 16 significant digits for non-terminating divisions such as 1 / 3. */
    private static final MathContext DIVISION_PRECISION = MathContext.DECIMAL64;

    public BigDecimal add(BigDecimal a, BigDecimal b) {
        return normalize(a.add(b));
    }

    public BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return normalize(a.subtract(b));
    }

    public BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return normalize(a.multiply(b));
    }

    public BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (b.signum() == 0) {
            throw new DivisionByZeroException();
        }
        return normalize(a.divide(b, DIVISION_PRECISION));
    }

    /**
     * Removes insignificant trailing zeros (2.50 -> 2.5, 6.0 -> 6) while never switching to
     * scientific notation (100 stays 100, not 1E+2).
     */
    static BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
