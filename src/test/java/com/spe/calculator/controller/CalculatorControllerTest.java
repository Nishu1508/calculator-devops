package com.spe.calculator.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spe.calculator.exception.GlobalExceptionHandler;
import com.spe.calculator.service.CalculatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer slice test: starts only the MVC infrastructure plus the controller, service and
 * exception handler (no full application context, no real HTTP server).
 */
@WebMvcTest(CalculatorController.class)
@Import({CalculatorService.class, GlobalExceptionHandler.class})
class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "GET /api/v1/{0}?a={1}&b={2} -> {3}")
    @CsvSource({
        "add,      2,   3,  5",
        "subtract, 10,  4,  6",
        "multiply, 6,   7,  42",
        "divide,   7,   2,  3.5",
        "add,      0.1, 0.2, 0.3",
        "subtract, -5,  -5, 0"
    })
    void returnsResultForEveryOperation(String op, String a, String b, String expected) throws Exception {
        mockMvc.perform(get("/api/v1/" + op).param("a", a).param("b", b))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.operation").value(op))
                .andExpect(jsonPath("$.result").value(Double.parseDouble(expected)));
    }

    @Test
    void responseEchoesInputs() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "2").param("b", "3"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"operation\":\"add\",\"a\":2,\"b\":3,\"result\":5}", true));
    }

    @Test
    void divideByZeroReturns400WithErrorJson() throws Exception {
        mockMvc.perform(get("/api/v1/divide").param("a", "10").param("b", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Division by zero")))
                .andExpect(jsonPath("$.path").value("/api/v1/divide"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @ParameterizedTest(name = "missing parameter on /{0}")
    @ValueSource(strings = {"add", "subtract", "multiply", "divide"})
    void missingParameterReturns400(String op) throws Exception {
        mockMvc.perform(get("/api/v1/" + op).param("a", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'b' is missing or empty"));
    }

    @Test
    void emptyParameterReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "").param("b", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'a' is missing or empty"));
    }

    @ParameterizedTest(name = "a={0} is rejected")
    @ValueSource(strings = {"abc", "1.2.3", "NaN", "Infinity", "12abc"})
    void nonNumericParameterReturns400(String badValue) throws Exception {
        mockMvc.perform(get("/api/v1/multiply").param("a", badValue).param("b", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Parameter 'a' must be a valid number but was '" + badValue + "'"));
    }
}
