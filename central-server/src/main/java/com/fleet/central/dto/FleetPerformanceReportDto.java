package com.fleet.central.dto;

import java.util.Map;

public class FleetPerformanceReportDto {

    private long totalScooters;
    private long availableScooters;
    private long reservedScooters;
    private long movingScooters;
    private long maintenanceScooters;

    private double averageBatteryPercent;
    private int minBatteryPercent;
    private int maxBatteryPercent;
    private long lowBatteryAlertCount;
    private Map<String, Long> batteryDistribution;

    private long totalRides;
    private long activeRides;
    private long completedRides;
    private long totalRevenueCents;
    private double totalRevenueEur;
    private double parkingCompliancePercent;

    private String generatedAt;

    public FleetPerformanceReportDto() {
    }

    public FleetPerformanceReportDto(long totalScooters, long availableScooters, long reservedScooters, long movingScooters,
                                     long maintenanceScooters, double averageBatteryPercent, int minBatteryPercent,
                                     int maxBatteryPercent, long lowBatteryAlertCount, Map<String, Long> batteryDistribution,
                                     long totalRides, long activeRides, long completedRides, long totalRevenueCents,
                                     double totalRevenueEur, double parkingCompliancePercent, String generatedAt) {
        this.totalScooters = totalScooters;
        this.availableScooters = availableScooters;
        this.reservedScooters = reservedScooters;
        this.movingScooters = movingScooters;
        this.maintenanceScooters = maintenanceScooters;
        this.averageBatteryPercent = averageBatteryPercent;
        this.minBatteryPercent = minBatteryPercent;
        this.maxBatteryPercent = maxBatteryPercent;
        this.lowBatteryAlertCount = lowBatteryAlertCount;
        this.batteryDistribution = batteryDistribution;
        this.totalRides = totalRides;
        this.activeRides = activeRides;
        this.completedRides = completedRides;
        this.totalRevenueCents = totalRevenueCents;
        this.totalRevenueEur = totalRevenueEur;
        this.parkingCompliancePercent = parkingCompliancePercent;
        this.generatedAt = generatedAt;
    }

    public long getTotalScooters() {
        return totalScooters;
    }

    public void setTotalScooters(long totalScooters) {
        this.totalScooters = totalScooters;
    }

    public long getAvailableScooters() {
        return availableScooters;
    }

    public void setAvailableScooters(long availableScooters) {
        this.availableScooters = availableScooters;
    }

    public long getReservedScooters() {
        return reservedScooters;
    }

    public void setReservedScooters(long reservedScooters) {
        this.reservedScooters = reservedScooters;
    }

    public long getMovingScooters() {
        return movingScooters;
    }

    public void setMovingScooters(long movingScooters) {
        this.movingScooters = movingScooters;
    }

    public long getMaintenanceScooters() {
        return maintenanceScooters;
    }

    public void setMaintenanceScooters(long maintenanceScooters) {
        this.maintenanceScooters = maintenanceScooters;
    }

    public double getAverageBatteryPercent() {
        return averageBatteryPercent;
    }

    public void setAverageBatteryPercent(double averageBatteryPercent) {
        this.averageBatteryPercent = averageBatteryPercent;
    }

    public int getMinBatteryPercent() {
        return minBatteryPercent;
    }

    public void setMinBatteryPercent(int minBatteryPercent) {
        this.minBatteryPercent = minBatteryPercent;
    }

    public int getMaxBatteryPercent() {
        return maxBatteryPercent;
    }

    public void setMaxBatteryPercent(int maxBatteryPercent) {
        this.maxBatteryPercent = maxBatteryPercent;
    }

    public long getLowBatteryAlertCount() {
        return lowBatteryAlertCount;
    }

    public void setLowBatteryAlertCount(long lowBatteryAlertCount) {
        this.lowBatteryAlertCount = lowBatteryAlertCount;
    }

    public Map<String, Long> getBatteryDistribution() {
        return batteryDistribution;
    }

    public void setBatteryDistribution(Map<String, Long> batteryDistribution) {
        this.batteryDistribution = batteryDistribution;
    }

    public long getTotalRides() {
        return totalRides;
    }

    public void setTotalRides(long totalRides) {
        this.totalRides = totalRides;
    }

    public long getActiveRides() {
        return activeRides;
    }

    public void setActiveRides(long activeRides) {
        this.activeRides = activeRides;
    }

    public long getCompletedRides() {
        return completedRides;
    }

    public void setCompletedRides(long completedRides) {
        this.completedRides = completedRides;
    }

    public long getTotalRevenueCents() {
        return totalRevenueCents;
    }

    public void setTotalRevenueCents(long totalRevenueCents) {
        this.totalRevenueCents = totalRevenueCents;
    }

    public double getTotalRevenueEur() {
        return totalRevenueEur;
    }

    public void setTotalRevenueEur(double totalRevenueEur) {
        this.totalRevenueEur = totalRevenueEur;
    }

    public double getParkingCompliancePercent() {
        return parkingCompliancePercent;
    }

    public void setParkingCompliancePercent(double parkingCompliancePercent) {
        this.parkingCompliancePercent = parkingCompliancePercent;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }
}
