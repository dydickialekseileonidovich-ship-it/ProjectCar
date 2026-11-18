package com.example.fleet.model;

import jakarta.persistence.*;
import java.util.Set;

/**
 * JPA-сущность "Водитель". Хранится в таблице "drivers".
 */
@Entity
@Table(name = "drivers")
public class Driver {
    // Первичный ключ (ID), генерируется базой данных автоматически (Auto-Increment)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ФИО водителя (обязательное поле в БД: NOT NULL)
    @Column(nullable = false)
    private String fullName;

    // Флаг занятости: true — водитель сейчас в рейсе (не может взять вторую машину одновременно)
    private boolean busy = false;

    // Набор открытых категорий прав у водителя (связь 1-ко-многим через отдельную таблицу driver_categories).
    // FetchType.EAGER загружает категории сразу вместе с водителем.
    @ElementCollection(targetClass = VehicleCategory.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "driver_categories", joinColumns = @JoinColumn(name = "driver_id"))
    @Enumerated(EnumType.STRING) // Храним названия категорий строкой ("CAR", "TRUCK"), а не числом
    private Set<VehicleCategory> categories;

    // Пустой конструктор обязателен для работы Hibernate (JPA)
    public Driver() {}

    public Driver(String fullName, Set<VehicleCategory> categories) {
        this.fullName = fullName;
        this.categories = categories;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public boolean isBusy() { return busy; }
    public void setBusy(boolean busy) { this.busy = busy; }
    public Set<VehicleCategory> getCategories() { return categories; }
}
