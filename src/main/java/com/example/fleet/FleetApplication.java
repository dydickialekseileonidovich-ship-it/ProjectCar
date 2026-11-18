package com.example.fleet;

import com.example.fleet.model.*;
import com.example.fleet.repository.*;
import com.example.fleet.service.FleetService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Главный класс запуска Spring Boot приложения.
 */
@SpringBootApplication
public class FleetApplication {

    public static void main(String[] args) {
        SpringApplication.run(FleetApplication.class, args);
    }

    /**
     * Демонстрационный сценарий (выполняется автоматически сразу после старта приложения).
     * Показывает полный жизненный цикл работы автопарка и выводит отчеты в консоль.
     */
    @Bean
    public CommandLineRunner demo(DriverRepository driverRepo,
                                  VehicleRepository vehicleRepo,
                                  FleetService fleetService) {
        return args -> {
            System.out.println("=== ЗАПУСК ДЕМОНСТРАЦИИ АВТОПАРКА ===");

            // Регистрируем водителей с их открытыми категориями прав
            Driver d1 = driverRepo.save(new Driver("Иван Петров", Set.of(VehicleCategory.CAR, VehicleCategory.TRUCK)));
            Driver d2 = driverRepo.save(new Driver("Анна Сидорова", Set.of(VehicleCategory.BUS)));

            // Регистрируем автомобили в автопарке (изначально в статусе AVAILABLE)
            Vehicle v1 = vehicleRepo.save(new Vehicle("A111AA", "Volvo FH16", VehicleCategory.TRUCK));
            Vehicle v2 = vehicleRepo.save(new Vehicle("B222BB", "Toyota Camry", VehicleCategory.CAR));

            // 1. Начинаем ночной рейс в понедельник в 02:00 (выезд разрешен круглосуточно 24/7)
            LocalDateTime start1 = LocalDateTime.of(2026, 11, 16, 2, 0);
            Trip trip1 = fleetService.startTrip(d1.getId(), v1.getId(), start1);
            System.out.println("Рейс начат: " + d1.getFullName() + " на " + v1.getModel());

            // 2. Завершаем рейс через 6 часов (в 08:00). Машина переходит в статус NEEDS_MAINTENANCE
            fleetService.finishTrip(trip1.getId(), start1.plusHours(6));
            System.out.println("Рейс завершен. Статус авто: ТРЕБУЕТ ОБСЛУЖИВАНИЯ");

            // 3. Проводим обслуживание в будний день (понедельник) в 10:00 (входит в окно 08:00–17:00)
            fleetService.performMaintenance(v1.getId(), "Мойка и заправка", LocalDateTime.of(2026, 11, 16, 10, 0));
            System.out.println("Обслуживание пройдено. Авто снова доступно!");

            // 4. Водитель совершает второй рейс (4 часа) на легковом автомобиле Toyota Camry
            LocalDateTime start2 = LocalDateTime.of(2026, 11, 16, 12, 0);
            Trip trip2 = fleetService.startTrip(d1.getId(), v2.getId(), start2);
            fleetService.finishTrip(trip2.getId(), start2.plusHours(4));

            // 5. Выводим итоговые аналитические отчеты
            System.out.println("\n--- ОТЧЕТ ПО РАБОЧЕМУ ВРЕМЕНИ ВОДИТЕЛЕЙ (ЧАСЫ) ---");
            fleetService.getDriverWorkHoursReport().forEach((k, v) -> System.out.println(k + ": " + v + " ч."));

            System.out.println("\n--- ОТЧЕТ ПО ИСПОЛЬЗОВАНИЮ АВТОМОБИЛЕЙ (ПОЕЗДКИ) ---");
            fleetService.getVehicleUsageReport().forEach((k, v) -> System.out.println(k + ": " + v + " рейс(ов)"));
        };
    }
}
