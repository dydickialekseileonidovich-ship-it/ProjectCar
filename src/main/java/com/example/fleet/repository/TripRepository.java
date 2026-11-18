package com.example.fleet.repository;

import com.example.fleet.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для работы с таблицей рейсов ("trips").
 */
public interface TripRepository extends JpaRepository<Trip, Long> {}
