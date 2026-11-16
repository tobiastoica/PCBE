package com.fleet.central.dto;

import java.util.List;

public class RebalanceProposalRequest {
    private String sourceStationId;
    private String targetStationId;
    private List<String> scooterIds;
    private String justification;

    public RebalanceProposalRequest() {}

    public RebalanceProposalRequest(String sourceStationId, String targetStationId, List<String> scooterIds, String justification) {
        this.sourceStationId = sourceStationId;
        this.targetStationId = targetStationId;
        this.scooterIds = scooterIds;
        this.justification = justification;
    }

    public String getSourceStationId() { return sourceStationId; }
    public void setSourceStationId(String sourceStationId) { this.sourceStationId = sourceStationId; }

    public String getTargetStationId() { return targetStationId; }
    public void setTargetStationId(String targetStationId) { this.targetStationId = targetStationId; }

    public List<String> getScooterIds() { return scooterIds; }
    public void setScooterIds(List<String> scooterIds) { this.scooterIds = scooterIds; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
}
