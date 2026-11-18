package com.example.fleet.model;

/**
 * Перечисление текущих состояний (статусов) автомобиля в автопарке.
 * Жизненный цикл: AVAILABLE -> IN_USE -> NEEDS_MAINTENANCE -> AVAILABLE.
 */
public enum VehicleStatus {
    AVAILABLE,          // Свободна, обслужена и готова к выезду в рейс
    IN_USE,             // Находится в рейсе прямо сейчас
    NEEDS_MAINTENANCE   // Вернулась из рейса и обязана пройти ТО перед следующей поездкой
}
