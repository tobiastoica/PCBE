package com.fleet.central.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "rebalance_tasks")
public class RebalanceTask {

    @Id
    @Column(name = "task_id", length = 36)
    private String taskId;

    @Column(name = "source_station_id", length = 32, nullable = false)
    private String sourceStationId;

    @Column(name = "target_station_id", length = 32, nullable = false)
    private String targetStationId;

    @Column(name = "scooter_ids", length = 1024, nullable = false)
    private String scooterIdsRaw; // Stored as comma-separated IDs for cross-DB compatibility

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 24, nullable = false)
    private RebalanceStatus status = RebalanceStatus.PROPOSED;

    @Column(name = "proposed_by", length = 64, nullable = false)
    private String proposedBy;

    @Column(name = "justification", length = 2048, nullable = false)
    private String justification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RebalanceTask() {}

    public RebalanceTask(String taskId, String sourceStationId, String targetStationId, List<String> scooterIds,
                         String proposedBy, String justification) {
        this.taskId = taskId;
        this.sourceStationId = sourceStationId;
        this.targetStationId = targetStationId;
        this.scooterIdsRaw = scooterIds != null ? String.join(",", scooterIds) : "";
        this.proposedBy = proposedBy;
        this.justification = justification;
        this.status = RebalanceStatus.PROPOSED;
        this.createdAt = Instant.now();
    }

    public List<String> getScooterIds() {
        if (scooterIdsRaw == null || scooterIdsRaw.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.asList(scooterIdsRaw.split(","));
    }

    public void setScooterIds(List<String> scooterIds) {
        this.scooterIdsRaw = scooterIds != null ? String.join(",", scooterIds) : "";
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getSourceStationId() { return sourceStationId; }
    public void setSourceStationId(String sourceStationId) { this.sourceStationId = sourceStationId; }

    public String getTargetStationId() { return targetStationId; }
    public void setTargetStationId(String targetStationId) { this.targetStationId = targetStationId; }

    public String getScooterIdsRaw() { return scooterIdsRaw; }
    public void setScooterIdsRaw(String scooterIdsRaw) { this.scooterIdsRaw = scooterIdsRaw; }

    public RebalanceStatus getStatus() { return status; }
    public void setStatus(RebalanceStatus status) { this.status = status; }

    public String getProposedBy() { return proposedBy; }
    public void setProposedBy(String proposedBy) { this.proposedBy = proposedBy; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
