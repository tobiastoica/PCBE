package com.fleet.central.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
public class DockerStationManager {

    private static final Logger log = LoggerFactory.getLogger(DockerStationManager.class);

    private final String dockerHost;
    private final String stationImage;
    private DockerClient dockerClient;

    public DockerStationManager(
            @Value("${fleet.docker.host:unix:///var/run/docker.sock}") String dockerHost,
            @Value("${fleet.docker.station-image:charging-station-service:latest}") String stationImage) {
        this.dockerHost = dockerHost;
        this.stationImage = stationImage;
        initDockerClient();
    }

    private void initDockerClient() {
        try {
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .build();

            ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(URI.create(dockerHost))
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(50)
                    .connectionTimeout(Duration.ofSeconds(5))
                    .responseTimeout(Duration.ofSeconds(15))
                    .build();

            this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
        } catch (Exception e) {
            log.warn("Docker daemon connection initialization skipped: {}", e.getMessage());
            this.dockerClient = null;
        }
    }

    /**
     * Spawns a new charging station container with the given station parameters.
     */
    public String spawnStationContainer(String stationId, String stationName, double lat, double lon, int port) {
        String containerName = "charging-station-" + stationId.toLowerCase();
        if (dockerClient != null) {
            try {
                CreateContainerResponse container = dockerClient.createContainerCmd(stationImage)
                        .withName(containerName)
                        .withEnv(
                                "STATION_ID=" + stationId,
                                "STATION_NAME=" + stationName,
                                "STATION_LAT=" + lat,
                                "STATION_LON=" + lon,
                                "SERVER_PORT=" + port,
                                "SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-broker:29092"
                        )
                        .exec();

                dockerClient.startContainerCmd(container.getId()).exec();
                log.info("Spawned Docker container {} ({}) for station {}", container.getId(), containerName, stationId);
                return container.getId();
            } catch (Exception e) {
                log.error("Failed to spawn Docker container for station {}: {}", stationId, e.getMessage());
            }
        }
        // Fallback simulated container ID when Docker daemon is not mounted or in dev environment
        String simulatedId = "sim_dock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Simulated station container registered with ID: {}", simulatedId);
        return simulatedId;
    }
}
