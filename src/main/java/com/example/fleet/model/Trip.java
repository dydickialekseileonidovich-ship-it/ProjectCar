package com.example.fleet.model;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * JPA-сущность "Рейс" (поездка). Хранится в таблице "trips".
 * Связывает водителя, автомобиль и временной интервал поездки.
 */
@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Многие рейсы могут относиться к одному водителю (внешний ключ driver_id)
    @ManyToOne(optional = false)
    private Driver driver;

    // Многие рейсы могут выполняться на одном автомобиле (внешний ключ vehicle_id)
    @ManyToOne(optional = false)
    private Vehicle vehicle;

    // Дата и время начала рейса
    @Column(nullable = false)
    private LocalDateTime startTime;

    // Дата и время завершения рейса (пока рейс не завершен, значение равно null)
    private LocalDateTime endTime;

    public Trip() {}

    public Trip(Driver driver, Vehicle vehicle, LocalDateTime startTime) {
        this.driver = driver;
        this.vehicle = vehicle;
        this.startTime = startTime;
    }

    /**
     * Вычисляет продолжительность завершенного рейса в полных часах (для отчета по выработке водителей).
     */
    public long getDurationHours() {
        if (startTime == null || endTime == null) return 0;
        return Duration.between(startTime, endTime).toHours();
    }

    public Long getId() { return id; }
    public Driver getDriver() { return driver; }
    public Vehicle getVehicle() { return vehicle; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
