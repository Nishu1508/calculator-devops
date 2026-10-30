package com.spe.calculator.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Spring MVC integration test: boots the complete application context (controllers, service,
 * exception handler, Actuator) and drives real endpoints through MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CalculatorApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void addEndpointWorksEndToEnd() throws Exception {
        mockMvc.perform(get("/api/v1/add?a=2&b=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("add"))
                .andExpect(jsonPath("$.a").value(2))
                .andExpect(jsonPath("$.b").value(3))
                .andExpect(jsonPath("$.result").value(5));
    }

    @Test
    void divideByZeroIsRejectedEndToEnd() throws Exception {
        mockMvc.perform(get("/api/v1/divide?a=1&b=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void actuatorHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void unknownEndpointReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/power?a=2&b=3")).andExpect(status().isNotFound());
    }
}
