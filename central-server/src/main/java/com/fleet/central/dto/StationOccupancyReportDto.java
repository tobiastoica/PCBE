package com.fleet.central.dto;

import java.util.List;

public class StationOccupancyReportDto {

    private int totalStations;
    private int totalCapacity;
    private long totalDocked;
    private double averageOccupancyPercent;
    private List<StationOccupancyItemDto> stations;

    public StationOccupancyReportDto() {
    }

    public StationOccupancyReportDto(int totalStations, int totalCapacity, long totalDocked, double averageOccupancyPercent, List<StationOccupancyItemDto> stations) {
        this.totalStations = totalStations;
        this.totalCapacity = totalCapacity;
        this.totalDocked = totalDocked;
        this.averageOccupancyPercent = averageOccupancyPercent;
        this.stations = stations;
    }

    public int getTotalStations() {
        return totalStations;
    }

    public void setTotalStations(int totalStations) {
        this.totalStations = totalStations;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getTotalDocked() {
        return totalDocked;
    }

    public void setTotalDocked(long totalDocked) {
        this.totalDocked = totalDocked;
    }

    public double getAverageOccupancyPercent() {
        return averageOccupancyPercent;
    }

    public void setAverageOccupancyPercent(double averageOccupancyPercent) {
        this.averageOccupancyPercent = averageOccupancyPercent;
    }

    public List<StationOccupancyItemDto> getStations() {
        return stations;
    }

    public void setStations(List<StationOccupancyItemDto> stations) {
        this.stations = stations;
    }

    public static class StationOccupancyItemDto {
        private String stationId;
        private String name;
        private int capacity;
        private long dockedCount;
        private double occupancyPercent;
        private String status;
        private boolean isFull;
        private boolean isEmpty;
        private int surplusOrDeficit;
        private List<String> dockedScooterIds;

        public StationOccupancyItemDto() {
        }

        public StationOccupancyItemDto(String stationId, String name, int capacity, long dockedCount,
                                       double occupancyPercent, String status, boolean isFull, boolean isEmpty,
                                       int surplusOrDeficit, List<String> dockedScooterIds) {
            this.stationId = stationId;
            this.name = name;
            this.capacity = capacity;
            this.dockedCount = dockedCount;
            this.occupancyPercent = occupancyPercent;
            this.status = status;
            this.isFull = isFull;
            this.isEmpty = isEmpty;
            this.surplusOrDeficit = surplusOrDeficit;
            this.dockedScooterIds = dockedScooterIds;
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

        public double getOccupancyPercent() {
            return occupancyPercent;
        }

        public void setOccupancyPercent(double occupancyPercent) {
            this.occupancyPercent = occupancyPercent;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public boolean isFull() {
            return isFull;
        }

        public void setFull(boolean full) {
            isFull = full;
        }

        public boolean isEmpty() {
            return isEmpty;
        }

        public void setEmpty(boolean empty) {
            isEmpty = empty;
        }

        public int getSurplusOrDeficit() {
            return surplusOrDeficit;
        }

        public void setSurplusOrDeficit(int surplusOrDeficit) {
            this.surplusOrDeficit = surplusOrDeficit;
        }

        public List<String> getDockedScooterIds() {
            return dockedScooterIds;
        }

        public void setDockedScooterIds(List<String> dockedScooterIds) {
            this.dockedScooterIds = dockedScooterIds;
        }
    }
}
