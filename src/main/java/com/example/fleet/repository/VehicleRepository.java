package com.example.fleet.repository;

import com.example.fleet.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для работы с таблицей автомобилей ("vehicles").
 */
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {}
