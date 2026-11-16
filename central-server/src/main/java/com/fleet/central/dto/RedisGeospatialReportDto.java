package com.fleet.central.dto;

import java.util.List;

public class RedisGeospatialReportDto {

    private String keyName;
    private long totalIndexedScooters;
    private List<ScooterNearbyDto> indexedScooters;
    private long metadataKeysCount;
    private String status;
    private String queryTimestamp;

    public RedisGeospatialReportDto() {
    }

    public RedisGeospatialReportDto(String keyName, long totalIndexedScooters, List<ScooterNearbyDto> indexedScooters,
                                    long metadataKeysCount, String status, String queryTimestamp) {
        this.keyName = keyName;
        this.totalIndexedScooters = totalIndexedScooters;
        this.indexedScooters = indexedScooters;
        this.metadataKeysCount = metadataKeysCount;
        this.status = status;
        this.queryTimestamp = queryTimestamp;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public long getTotalIndexedScooters() {
        return totalIndexedScooters;
    }

    public void setTotalIndexedScooters(long totalIndexedScooters) {
        this.totalIndexedScooters = totalIndexedScooters;
    }

    public List<ScooterNearbyDto> getIndexedScooters() {
        return indexedScooters;
    }

    public void setIndexedScooters(List<ScooterNearbyDto> indexedScooters) {
        this.indexedScooters = indexedScooters;
    }

    public long getMetadataKeysCount() {
        return metadataKeysCount;
    }

    public void setMetadataKeysCount(long metadataKeysCount) {
        this.metadataKeysCount = metadataKeysCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQueryTimestamp() {
        return queryTimestamp;
    }

    public void setQueryTimestamp(String queryTimestamp) {
        this.queryTimestamp = queryTimestamp;
    }
}
