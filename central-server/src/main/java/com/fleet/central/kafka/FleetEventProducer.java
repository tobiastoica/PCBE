package com.fleet.central.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FleetEventProducer {

    private static final Logger log = LoggerFactory.getLogger(FleetEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FleetEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishAdminTransfer(String sourceStationId, String targetStationId, List<String> scooterIds, String taskId, String reason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("commandId", "cmd_" + UUID.randomUUID().toString().substring(0, 8));
            event.put("commandType", "DISPATCH_TRANSFER");
            event.put("timestamp", Instant.now().toString());
            event.put("sourceStationId", sourceStationId);
            event.put("targetStationId", targetStationId);
            event.put("scooterIds", scooterIds);
            event.put("taskId", taskId);
            event.put("reason", reason);

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopicConfig.TOPIC_ADMIN_TRANSFER, sourceStationId, json);
            log.info("Published admin transfer command to Kafka: {}", json);
        } catch (Exception e) {
            log.error("Failed to publish admin transfer command: {}", e.getMessage(), e);
        }
    }
}
