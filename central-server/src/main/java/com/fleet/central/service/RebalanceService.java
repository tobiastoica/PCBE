package com.fleet.central.service;

import com.fleet.central.domain.RebalanceStatus;
import com.fleet.central.domain.RebalanceTask;
import com.fleet.central.dto.DemandPredictionDto;
import com.fleet.central.dto.RebalanceProposalRequest;
import com.fleet.central.dto.RebalanceTaskDto;
import com.fleet.central.kafka.FleetEventProducer;
import com.fleet.central.repository.ChargingStationRepository;
import com.fleet.central.repository.RebalanceTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RebalanceService {

    private static final Logger log = LoggerFactory.getLogger(RebalanceService.class);

    private final RebalanceTaskRepository rebalanceTaskRepository;
    private final ChargingStationRepository stationRepository;
    private final FleetEventProducer fleetEventProducer;

    public RebalanceService(RebalanceTaskRepository rebalanceTaskRepository,
                            ChargingStationRepository stationRepository,
                            FleetEventProducer fleetEventProducer) {
        this.rebalanceTaskRepository = rebalanceTaskRepository;
        this.stationRepository = stationRepository;
        this.fleetEventProducer = fleetEventProducer;
    }

    public List<RebalanceTaskDto> getPendingProposals() {
        List<RebalanceTask> tasks = rebalanceTaskRepository.findByStatusOrderByCreatedAtDesc(RebalanceStatus.PROPOSED);
        List<RebalanceTaskDto> dtos = new ArrayList<>();
        for (RebalanceTask t : tasks) {
            dtos.add(new RebalanceTaskDto(
                    t.getTaskId(),
                    t.getSourceStationId(),
                    t.getTargetStationId(),
                    t.getScooterIds(),
                    t.getStatus().name(),
                    t.getProposedBy(),
                    t.getJustification(),
                    t.getCreatedAt().toString()
            ));
        }
        return dtos;
    }

    @Transactional
    public RebalanceTaskDto approveProposal(String taskId) {
        RebalanceTask task = rebalanceTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Rebalance task not found: " + taskId));

        task.setStatus(RebalanceStatus.APPROVED);
        rebalanceTaskRepository.save(task);

        // Publish event to Kafka to dispatch physical rebalancing
        fleetEventProducer.publishAdminTransfer(
                task.getSourceStationId(),
                task.getTargetStationId(),
                task.getScooterIds(),
                task.getTaskId(),
                task.getJustification()
        );

        log.info("Rebalance proposal {} approved and published to Kafka.", taskId);

        return new RebalanceTaskDto(
                task.getTaskId(),
                task.getSourceStationId(),
                task.getTargetStationId(),
                task.getScooterIds(),
                task.getStatus().name(),
                task.getProposedBy(),
                task.getJustification(),
                task.getCreatedAt().toString()
        );
    }

    @Transactional
    public RebalanceTaskDto proposeRebalance(RebalanceProposalRequest request, String proposedBy) {
        String taskId = "reb_" + UUID.randomUUID().toString().substring(0, 8);
        RebalanceTask task = new RebalanceTask(
                taskId,
                request.getSourceStationId(),
                request.getTargetStationId(),
                request.getScooterIds(),
                proposedBy != null ? proposedBy : "AUTONOMOUS_AGENT",
                request.getJustification()
        );
        rebalanceTaskRepository.save(task);

        log.info("New rebalance proposal {} recorded by {}.", taskId, proposedBy);

        return new RebalanceTaskDto(
                task.getTaskId(),
                task.getSourceStationId(),
                task.getTargetStationId(),
                task.getScooterIds(),
                task.getStatus().name(),
                task.getProposedBy(),
                task.getJustification(),
                task.getCreatedAt().toString()
        );
    }

    public DemandPredictionDto predictDemand(String stationId, int targetHour) {
        // Deterministic urban commuter demand model:
        // Morning rush peak (08:00): 25-30 scooters
        // Evening rush peak (18:00): 20-25 scooters
        // Mid-day normal: 10-15
        int predicted;
        int historical;

        if (targetHour >= 7 && targetHour <= 9) {
            predicted = 28;
            historical = 25;
        } else if (targetHour >= 16 && targetHour <= 19) {
            predicted = 22;
            historical = 20;
        } else if (targetHour >= 0 && targetHour <= 5) {
            predicted = 3;
            historical = 4;
        } else {
            predicted = 14;
            historical = 12;
        }

        return new DemandPredictionDto(stationId, targetHour, predicted, historical);
    }
}
