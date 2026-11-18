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
 * Отвечает за проверку правил выдачи автомобилей, завершения рейсов, проведения ТО и построения отчетов.
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
        // 1. Ищем водителя и автомобиль в БД по их ID
        Driver driver = driverRepo.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Водитель не найден"));
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Автомобиль не найден"));

        // 2. ПРОВЕРКА ПРАВИЛА №1: Один водитель не может управлять несколькими авто одновременно
        if (driver.isBusy()) {
            throw new IllegalStateException("Водитель уже находится в другом рейсе!");
        }
        // 3. ПРОВЕРКА ПРАВИЛА №2: У водителя должна быть открыта категория прав для данного вида ТС
        if (!driver.getCategories().contains(vehicle.getCategory())) {
            throw new IllegalStateException("У водителя нет нужной категории прав: " + vehicle.getCategory());
        }
        // 4. ПРОВЕРКА ПРАВИЛА №3: Автомобиль должен быть свободен и обслужен (статус AVAILABLE)
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException("Автомобиль недоступен (в рейсе или требует обслуживания)!");
        }

        // 5. Меняем состояния: водитель занят, машина в рейсе (IN_USE)
        driver.setBusy(true);
        vehicle.setStatus(VehicleStatus.IN_USE);

        driverRepo.save(driver);
        vehicleRepo.save(vehicle);

        // 6. Создаем и сохраняем запись о начатом рейсе в БД
        return tripRepo.save(new Trip(driver, vehicle, startTime));
    }

    /**
     * Завершение рейса и возврат автомобиля в автопарк.
     */
    @Transactional
    public Trip finishTrip(Long tripId, LocalDateTime endTime) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Рейс не найден"));

        // Проверка корректности времени окончания поездки
        if (endTime.isBefore(trip.getStartTime())) {
            throw new IllegalArgumentException("Время окончания не может быть раньше времени начала рейса");
        }

        trip.setEndTime(endTime);

        // Водитель освобождается и может брать другой свободный автомобиль
        Driver driver = trip.getDriver();
        driver.setBusy(false);

        // ВАЖНОЕ ПРАВИЛО: После любого рейса автомобиль переходит в статус NEEDS_MAINTENANCE (требует ТО).
        // До прохождения обслуживания он не сможет снова выехать в рейс.
        Vehicle vehicle = trip.getVehicle();
        vehicle.setStatus(VehicleStatus.NEEDS_MAINTENANCE);

        driverRepo.save(driver);
        vehicleRepo.save(vehicle);
        return tripRepo.save(trip);
    }

    /**
     * Проведение обязательного технического обслуживания (мойка, заправка, осмотр) после рейса.
     */
    @Transactional
    public MaintenanceRecord performMaintenance(Long vehicleId, String description, LocalDateTime time) {
        DayOfWeek day = time.getDayOfWeek();
        int hour = time.getHour();

        // ВАЖНОЕ ПРАВИЛО: Служба ТО работает ТОЛЬКО в будние дни (ПН–ПТ) с 08:00 до 17:00
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY || hour < 8 || hour >= 17) {
            throw new IllegalStateException("Обслуживание проводится только в будние дни с 08:00 до 17:00!");
        }

        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Автомобиль не найден"));

        // Обслуживать можно только машину, которая вернулась из рейса и ждет ТО
        if (vehicle.getStatus() != VehicleStatus.NEEDS_MAINTENANCE) {
            throw new IllegalStateException("Автомобиль не нуждается в обслуживании или находится в рейсе.");
        }

        // После ТО автомобиль снова становится доступным для новых поездок (AVAILABLE)
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicleRepo.save(vehicle);

        // Сохраняем запись в журнал ТО
        return maintenanceRepo.save(new MaintenanceRecord(vehicle, time, description));
    }

    /**
     * Отчет №1: Суммарное отработанное время по каждому водителю (в часах).
     * Использует Stream API для группировки завершенных рейсов по ФИО водителя.
     */
    public Map<String, Long> getDriverWorkHoursReport() {
        return tripRepo.findAll().stream()
                .filter(t -> t.getEndTime() != null) // Учитываем только завершенные рейсы
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
