package com.example.fleet.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA-сущность "Запись о техническом обслуживании". Хранится в таблице "maintenance_records".
 * Фиксирует факт прохождения автомобилем обязательного ТО (мойка, заправка, осмотр) после рейса.
 */
@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Автомобиль, который прошел обслуживание (связь Многие-к-Одному)
    @ManyToOne(optional = false)
    private Vehicle vehicle;

    // Дата и время проведения обслуживания (разрешено только в будни с 08:00 до 17:00)
    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Описание выполненных работ (например, "Мойка и заправка")
    @Column(nullable = false)
    private String description;

    public MaintenanceRecord() {}

    public MaintenanceRecord(Vehicle vehicle, LocalDateTime timestamp, String description) {
        this.vehicle = vehicle;
        this.timestamp = timestamp;
        this.description = description;
    }

    public Long getId() { return id; }
    public Vehicle getVehicle() { return vehicle; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
}
