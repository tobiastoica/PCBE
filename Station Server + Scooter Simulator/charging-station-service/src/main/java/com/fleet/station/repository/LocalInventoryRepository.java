package com.fleet.station.repository;

import com.fleet.station.domain.InventoryStatus;
import com.fleet.station.domain.LocalInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocalInventoryRepository extends JpaRepository<LocalInventory, String> {

    List<LocalInventory> findByStatus(InventoryStatus status);
}
