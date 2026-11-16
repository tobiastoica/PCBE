package com.fleet.central.dto;

public class DemandPredictionDto {
    private String stationId;
    private int targetHour;
    private int predictedDemand;
    private int historicalAvg;

    public DemandPredictionDto() {}

    public DemandPredictionDto(String stationId, int targetHour, int predictedDemand, int historicalAvg) {
        this.stationId = stationId;
        this.targetHour = targetHour;
        this.predictedDemand = predictedDemand;
        this.historicalAvg = historicalAvg;
    }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public int getTargetHour() { return targetHour; }
    public void setTargetHour(int targetHour) { this.targetHour = targetHour; }

    public int getPredictedDemand() { return predictedDemand; }
    public void setPredictedDemand(int predictedDemand) { this.predictedDemand = predictedDemand; }

    public int getHistoricalAvg() { return historicalAvg; }
    public void setHistoricalAvg(int historicalAvg) { this.historicalAvg = historicalAvg; }
}
