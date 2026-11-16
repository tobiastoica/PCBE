package com.fleet.central.dto;

import java.util.List;

public class RebalanceTaskDto {
    private String taskId;
    private String sourceStationId;
    private String targetStationId;
    private List<String> scooterIds;
    private String status;
    private String proposedBy;
    private String justification;
    private String createdAt;

    public RebalanceTaskDto() {}

    public RebalanceTaskDto(String taskId, String sourceStationId, String targetStationId, List<String> scooterIds,
                            String status, String proposedBy, String justification, String createdAt) {
        this.taskId = taskId;
        this.sourceStationId = sourceStationId;
        this.targetStationId = targetStationId;
        this.scooterIds = scooterIds;
        this.status = status;
        this.proposedBy = proposedBy;
        this.justification = justification;
        this.createdAt = createdAt;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getSourceStationId() { return sourceStationId; }
    public void setSourceStationId(String sourceStationId) { this.sourceStationId = sourceStationId; }

    public String getTargetStationId() { return targetStationId; }
    public void setTargetStationId(String targetStationId) { this.targetStationId = targetStationId; }

    public List<String> getScooterIds() { return scooterIds; }
    public void setScooterIds(List<String> scooterIds) { this.scooterIds = scooterIds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProposedBy() { return proposedBy; }
    public void setProposedBy(String proposedBy) { this.proposedBy = proposedBy; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
