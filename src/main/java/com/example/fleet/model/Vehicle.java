package com.example.fleet.model;

import jakarta.persistence.*;

/**
 * JPA-сущность "Автомобиль". Хранится в таблице "vehicles".
 */
@Entity
@Table(name = "vehicles")
public class Vehicle {
    // Первичный ключ (ID) автомобиля
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Государственный регистрационный номер (уникальный, не может быть пустым)
    @Column(unique = true, nullable = false)
    private String licensePlate;

    // Марка и модель автомобиля (например, "Volvo FH16")
    @Column(nullable = false)
    private String model;

    // Категория ТС, которая требуется от водителя для управления этим автомобилем
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleCategory category;

    // Текущий статус автомобиля (по умолчанию новый автомобиль свободен — AVAILABLE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    // Пустой конструктор для JPA/Hibernate
    public Vehicle() {}

    public Vehicle(String licensePlate, String model, VehicleCategory category) {
        this.licensePlate = licensePlate;
        this.model = model;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getLicensePlate() { return licensePlate; }
    public String getModel() { return model; }
    public VehicleCategory getCategory() { return category; }
    public VehicleStatus getStatus() { return status; }
    public void setStatus(VehicleStatus status) { this.status = status; }
}
