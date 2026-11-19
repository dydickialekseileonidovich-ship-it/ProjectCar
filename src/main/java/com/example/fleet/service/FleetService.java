package com.example.fleet.service;

import com.example.fleet.model.*;
import com.example.fleet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Основной сервис бизнес-логики управления автопарком.
 * Отвечает за проверку правил выдачи автомобилей, завершения рейсов, заправки в пути, проведения ТО и построения отчетов.
 */
@Service
public class FleetService {

    private final DriverRepository driverRepo;
    private final VehicleRepository vehicleRepo;
    private final TripRepository tripRepo;
    private final MaintenanceRepository maintenanceRepo;

    // Внедрение зависимостей (Dependency Injection) через конструктор
    public FleetService(DriverRepository driverRepo, VehicleRepository vehicleRepo,
                        TripRepository tripRepo, MaintenanceRepository maintenanceRepo) {
        this.driverRepo = driverRepo;
        this.vehicleRepo = vehicleRepo;
        this.tripRepo = tripRepo;
        this.maintenanceRepo = maintenanceRepo;
    }

    /**
     * Выход автомобиля в рейс.
     * @Transactional гарантирует атомарность: все обновления в БД выполнятся вместе либо откатятся при ошибке.
     */
    @Transactional
    public Trip startTrip(Long driverId, Long vehicleId, LocalDateTime startTime) {
        Driver driver = driverRepo.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Водитель не найден"));
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Автомобиль не найден"));

        // ПРОВЕРКА ПРАВИЛА №1: Один водитель не может управлять несколькими авто одновременно
        if (driver.isBusy()) {
            throw new IllegalStateException("Водитель уже находится в другом рейсе!");
        }
        // ПРОВЕРКА ПРАВИЛА №2: У водителя должна быть открыта категория прав для данного вида ТС
        if (!driver.getCategories().contains(vehicle.getCategory())) {
            throw new IllegalStateException("У водителя нет нужной категории прав: " + vehicle.getCategory());
        }
        // ПРОВЕРКА ПРАВИЛА №3: Автомобиль должен быть свободен и обслужен (статус AVAILABLE)
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException("Автомобиль недоступен (в рейсе или требует обслуживания)!");
        }

        driver.setBusy(true);
        vehicle.setStatus(VehicleStatus.IN_USE);

        driverRepo.save(driver);
        vehicleRepo.save(vehicle);

        return tripRepo.save(new Trip(driver, vehicle, startTime));
    }

    /**
     * Завершение рейса и возврат автомобиля в автопарк.
     */
    @Transactional
    public Trip finishTrip(Long tripId, LocalDateTime endTime) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Рейс не найден"));

        if (endTime.isBefore(trip.getStartTime())) {
            throw new IllegalArgumentException("Время окончания не может быть раньше времени начала рейса");
        }

        trip.setEndTime(endTime);

        Driver driver = trip.getDriver();
        driver.setBusy(false);

        // После возврата из рейса автомобиль переходит в статус NEEDS_MAINTENANCE (требует послерейсового ТО в парке)
        Vehicle vehicle = trip.getVehicle();
        vehicle.setStatus(VehicleStatus.NEEDS_MAINTENANCE);

        driverRepo.save(driver);
        vehicleRepo.save(vehicle);
        return tripRepo.save(trip);
    }

    /**
     * Внесение записи в список работ / ТО:
     * 1) Если авто находится В РЕЙСЕ (IN_USE) — разрешена дозаправка или путевые работы в дороге (24/7),
     *    при этом статус авто остается IN_USE (рейс продолжается).
     * 2) Если авто вернулось из рейса (NEEDS_MAINTENANCE) — проводится послерейсовое ТО в автопарке
     *    (только в будние дни ПН–ПТ с 08:00 до 17:00), после чего авто переходит в статус AVAILABLE.
     */
    @Transactional
    public MaintenanceRecord performMaintenance(Long vehicleId, String description, LocalDateTime time) {
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Автомобиль не найден"));

        // Сценарий 1: Дозаправка или экстренные работы в дороге, пока машина в активном рейсе (IN_USE)
        if (vehicle.getStatus() == VehicleStatus.IN_USE) {
            return maintenanceRepo.save(new MaintenanceRecord(vehicle, time, "[В рейсе] " + description));
        }

        // Сценарий 2: Обязательное послерейсовое ТО на базе автопарка (работает в будни с 08:00 до 17:00)
        DayOfWeek day = time.getDayOfWeek();
        int hour = time.getHour();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY || hour < 8 || hour >= 17) {
            throw new IllegalStateException("Обслуживание на базе проводится только в будние дни с 08:00 до 17:00!");
        }

        if (vehicle.getStatus() != VehicleStatus.NEEDS_MAINTENANCE) {
            throw new IllegalStateException("Автомобиль уже обслужен и свободен.");
        }

        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicleRepo.save(vehicle);

        return maintenanceRepo.save(new MaintenanceRecord(vehicle, time, description));
    }

    /**
     * Отчет №1: Суммарное отработанное время по каждому водителю (в часах).
     */
    public Map<String, Long> getDriverWorkHoursReport() {
        return tripRepo.findAll().stream()
                .filter(t -> t.getEndTime() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getDriver().getFullName(),
                        Collectors.summingLong(Trip::getDurationHours)
                ));
    }

    /**
     * Отчет №2: Интенсивность использования автомобилей (количество совершенных поездок по каждому авто).
     */
    public Map<String, Long> getVehicleUsageReport() {
        return tripRepo.findAll().stream()
                .collect(Collectors.groupingBy(
                        t -> t.getVehicle().getModel() + " (" + t.getVehicle().getLicensePlate() + ")",
                        Collectors.counting()
                ));
    }
}
