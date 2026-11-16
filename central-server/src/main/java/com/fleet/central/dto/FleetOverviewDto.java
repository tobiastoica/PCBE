package com.fleet.central.dto;

import java.util.List;

public class FleetOverviewDto {
    private long totalStations;
    private long totalScooters;
    private long availableScooters;
    private long activeRides;
    private long maintenanceScooters;
    private List<StationOverviewDto> stations;

    public FleetOverviewDto() {}

    public FleetOverviewDto(long totalStations, long totalScooters, long availableScooters, long activeRides,
                            long maintenanceScooters, List<StationOverviewDto> stations) {
        this.totalStations = totalStations;
        this.totalScooters = totalScooters;
        this.availableScooters = availableScooters;
        this.activeRides = activeRides;
        this.maintenanceScooters = maintenanceScooters;
        this.stations = stations;
    }

    public long getTotalStations() { return totalStations; }
    public void setTotalStations(long totalStations) { this.totalStations = totalStations; }

    public long getTotalScooters() { return totalScooters; }
    public void setTotalScooters(long totalScooters) { this.totalScooters = totalScooters; }

    public long getAvailableScooters() { return availableScooters; }
    public void setAvailableScooters(long availableScooters) { this.availableScooters = availableScooters; }

    public long getActiveRides() { return activeRides; }
    public void setActiveRides(long activeRides) { this.activeRides = activeRides; }

    public long getMaintenanceScooters() { return maintenanceScooters; }
    public void setMaintenanceScooters(long maintenanceScooters) { this.maintenanceScooters = maintenanceScooters; }

    public List<StationOverviewDto> getStations() { return stations; }
    public void setStations(List<StationOverviewDto> stations) { this.stations = stations; }
}
