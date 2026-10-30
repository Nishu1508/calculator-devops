package com.spe.calculator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spe.calculator.exception.DivisionByZeroException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Plain JUnit 5 unit tests: no Spring context, so they run in milliseconds. */
class CalculatorServiceTest {

    private final CalculatorService service = new CalculatorService();

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @Nested
    class Add {
        @ParameterizedTest(name = "{0} + {1} = {2}")
        @CsvSource({"2, 3, 5", "-4, 10, 6", "0, 0, 0", "0.1, 0.2, 0.3", "2.50, 2.50, 5"})
        void addsTwoNumbers(String a, String b, String expected) {
            assertThat(service.add(bd(a), bd(b))).isEqualTo(bd(expected));
        }
    }

    @Nested
    class Subtract {
        @ParameterizedTest(name = "{0} - {1} = {2}")
        @CsvSource({"10, 4, 6", "4, 10, -6", "-3, -3, 0", "5.5, 0.25, 5.25"})
        void subtractsSecondFromFirst(String a, String b, String expected) {
            assertThat(service.subtract(bd(a), bd(b))).isEqualTo(bd(expected));
        }
    }

    @Nested
    class Multiply {
        @ParameterizedTest(name = "{0} * {1} = {2}")
        @CsvSource({"6, 7, 42", "-3, 4, -12", "123456, 0, 0", "1.5, 4, 6", "10, 10, 100"})
        void multipliesTwoNumbers(String a, String b, String expected) {
            assertThat(service.multiply(bd(a), bd(b))).isEqualTo(bd(expected));
        }

        @Test
        void neverReturnsScientificNotation() {
            assertThat(service.multiply(bd("10"), bd("10")).toString()).isEqualTo("100");
        }
    }

    @Nested
    class Divide {
        @ParameterizedTest(name = "{0} / {1} = {2}")
        @CsvSource({"10, 2, 5", "7, 2, 3.5", "-9, 3, -3", "1, 4, 0.25", "0, 5, 0"})
        void dividesFirstBySecond(String a, String b, String expected) {
            assertThat(service.divide(bd(a), bd(b))).isEqualTo(bd(expected));
        }

        @Test
        void nonTerminatingResultIsRoundedTo16SignificantDigits() {
            assertThat(service.divide(bd("1"), bd("3"))).isEqualTo(bd("0.3333333333333333"));
        }

        @Test
        void divideByZeroThrows() {
            assertThatThrownBy(() -> service.divide(bd("5"), BigDecimal.ZERO))
                    .isInstanceOf(DivisionByZeroException.class)
                    .hasMessageContaining("Division by zero");
        }

        @Test
        void divideByZeroWithDecimalZeroAlsoThrows() {
            assertThatThrownBy(() -> service.divide(bd("5"), bd("0.000")))
                    .isInstanceOf(DivisionByZeroException.class);
        }
    }
}
