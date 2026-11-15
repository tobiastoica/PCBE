package com.fleet.simulator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class Stage1ScaffoldingTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Stage 1 - Simulator Context: Should load cleanly with all bean definitions")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.containsBean("scooterSimulatorApplication")).isTrue();
    }
}
