package com.fleet.central;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ScooterDiscoveryTest {

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Verify Proximity Search returns nearby stationary available scooters")
    void testNearbyScootersEndpoint() throws Exception {
        // Query near Timisoara center
        mockMvc.perform(get("/api/v1/scooters/nearby")
                        .param("lat", "45.7538")
                        .param("lon", "21.2257")
                        .param("radiusMeters", "1500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].scooterId").isNotEmpty())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("Verify Demand Prediction endpoint")
    void testDemandPredictionEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/demand")
                        .param("stationId", "station_a")
                        .param("targetHour", "8")
                        .header("X-API-KEY", "test-api-key-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedDemand").isNumber())
                .andExpect(jsonPath("$.stationId").value("station_a"));
    }
}
