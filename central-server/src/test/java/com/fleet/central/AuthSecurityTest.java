package com.fleet.central;

import com.fleet.central.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthSecurityTest {

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Verify JWT issuance and validation")
    void testJwtTokenGeneration() {
        String token = jwtService.generateToken("admin", "ROLE_ADMIN", "Administrator");
        assertNotNull(token);
        assertEquals("admin", jwtService.extractUsername(token));
        assertEquals("ROLE_ADMIN", jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token, "admin"));
    }

    @Test
    @DisplayName("Verify Login Endpoint returns JWT")
    void testLoginEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Verify Access Denied without Token on Admin Endpoints")
    void testAdminAccessDeniedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/fleet-overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify System Access via X-API-KEY header")
    void testSystemAccessViaApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/admin/fleet-overview")
                        .header("X-API-KEY", "test-api-key-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStations").isNumber());
    }
}
