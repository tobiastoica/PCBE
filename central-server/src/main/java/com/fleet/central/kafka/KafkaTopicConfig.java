package com.fleet.central.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_RIDE_LIFECYCLE = "ride-lifecycle-events";
    public static final String TOPIC_STATION_HANDOVER = "station-handover-events";
    public static final String TOPIC_ADMIN_TRANSFER = "admin-transfer-commands";
    public static final String TOPIC_SCOOTER_TELEMETRY = "scooter-telemetry-topic";

    @Bean
    public NewTopic rideLifecycleTopic() {
        return TopicBuilder.name(TOPIC_RIDE_LIFECYCLE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stationHandoverTopic() {
        return TopicBuilder.name(TOPIC_STATION_HANDOVER)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic adminTransferTopic() {
        return TopicBuilder.name(TOPIC_ADMIN_TRANSFER)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic scooterTelemetryTopic() {
        return TopicBuilder.name(TOPIC_SCOOTER_TELEMETRY)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
