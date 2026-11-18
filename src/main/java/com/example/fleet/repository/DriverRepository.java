package com.example.fleet.repository;

import com.example.fleet.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для работы с таблицей водителей ("drivers").
 * Наследование от JpaRepository автоматически дает методы save(), findById(), findAll(), delete() и др.
 */
public interface DriverRepository extends JpaRepository<Driver, Long> {}
