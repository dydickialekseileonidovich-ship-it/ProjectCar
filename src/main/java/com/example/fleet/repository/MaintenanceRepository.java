package com.example.fleet.repository;

import com.example.fleet.model.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для работы с таблицей технического обслуживания ("maintenance_records").
 */
public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, Long> {}
