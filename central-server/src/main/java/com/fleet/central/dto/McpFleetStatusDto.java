package com.fleet.central.dto;

import java.util.List;

public class McpFleetStatusDto {

    private List<McpStationStatusDto> stations;
    private List<McpScooterSummaryDto> scooters;

    public McpFleetStatusDto() {
    }

    public McpFleetStatusDto(List<McpStationStatusDto> stations, List<McpScooterSummaryDto> scooters) {
        this.stations = stations;
        this.scooters = scooters;
    }

    public List<McpStationStatusDto> getStations() {
        return stations;
    }

    public void setStations(List<McpStationStatusDto> stations) {
        this.stations = stations;
    }

    public List<McpScooterSummaryDto> getScooters() {
        return scooters;
    }

    public void setScooters(List<McpScooterSummaryDto> scooters) {
        this.scooters = scooters;
    }

    public static class McpStationStatusDto {
        private String stationId;
        private String name;
        private int capacity;
        private long dockedCount;
        private List<String> scooterIds;
        private int deficit;

        public McpStationStatusDto() {
        }

        public McpStationStatusDto(String stationId, String name, int capacity, long dockedCount, List<String> scooterIds, int deficit) {
            this.stationId = stationId;
            this.name = name;
            this.capacity = capacity;
            this.dockedCount = dockedCount;
            this.scooterIds = scooterIds;
            this.deficit = deficit;
        }

        public String getStationId() {
            return stationId;
        }

        public void setStationId(String stationId) {
            this.stationId = stationId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public long getDockedCount() {
            return dockedCount;
        }

        public void setDockedCount(long dockedCount) {
            this.dockedCount = dockedCount;
        }

        public List<String> getScooterIds() {
            return scooterIds;
        }

        public void setScooterIds(List<String> scooterIds) {
            this.scooterIds = scooterIds;
        }

        public int getDeficit() {
            return deficit;
        }

        public void setDeficit(int deficit) {
            this.deficit = deficit;
        }
    }

    public static class McpScooterSummaryDto {
        private String scooterId;
        private String model;
        private int batteryLevel;
        private String status;
        private String stationId;
        private double lat;
        private double lon;

        public McpScooterSummaryDto() {
        }

        public McpScooterSummaryDto(String scooterId, String model, int batteryLevel, String status, String stationId, double lat, double lon) {
            this.scooterId = scooterId;
            this.model = model;
            this.batteryLevel = batteryLevel;
            this.status = status;
            this.stationId = stationId;
            this.lat = lat;
            this.lon = lon;
        }

        public String getScooterId() {
            return scooterId;
        }

        public void setScooterId(String scooterId) {
            this.scooterId = scooterId;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getBatteryLevel() {
            return batteryLevel;
        }

        public void setBatteryLevel(int batteryLevel) {
            this.batteryLevel = batteryLevel;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStationId() {
            return stationId;
        }

        public void setStationId(String stationId) {
            this.stationId = stationId;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }
    }
}
