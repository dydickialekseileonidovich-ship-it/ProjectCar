package com.example.fleet.controller;

import com.example.fleet.model.*;
import com.example.fleet.repository.*;
import com.example.fleet.service.FleetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * REST-контроллер для веб-интерфейса управления автопарком (http://localhost:8080).
 * Обрабатывает HTTP-запросы из браузера и вызывает методы бизнес-логики FleetService.
 */
@RestController
@RequestMapping("/api")
public class FleetController {

    private final FleetService fleetService;
    private final DriverRepository driverRepo;
    private final VehicleRepository vehicleRepo;
    private final TripRepository tripRepo;
    private final MaintenanceRepository maintenanceRepo;

    public FleetController(FleetService fleetService,
                           DriverRepository driverRepo,
                           VehicleRepository vehicleRepo,
                           TripRepository tripRepo,
                           MaintenanceRepository maintenanceRepo) {
        this.fleetService = fleetService;
        this.driverRepo = driverRepo;
        this.vehicleRepo = vehicleRepo;
        this.tripRepo = tripRepo;
        this.maintenanceRepo = maintenanceRepo;
    }

    /**
     * Получение всех данных автопарка и отчетов одним запросом для отображения на главной панели.
     */
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("drivers", driverRepo.findAll());
        data.put("vehicles", vehicleRepo.findAll());
        data.put("trips", tripRepo.findAll());
        data.put("maintenances", maintenanceRepo.findAll());
        data.put("driverHoursReport", fleetService.getDriverWorkHoursReport());
        data.put("vehicleUsageReport", fleetService.getVehicleUsageReport());
        return data;
    }

    /**
     * Выезд автомобиля в рейс.
     */
    @PostMapping("/trips/start")
    public Trip startTrip(@RequestBody Map<String, String> payload) {
        Long driverId = Long.valueOf(payload.get("driverId"));
        Long vehicleId = Long.valueOf(payload.get("vehicleId"));
        LocalDateTime startTime = LocalDateTime.parse(payload.get("startTime"));
        return fleetService.startTrip(driverId, vehicleId, startTime);
    }

    /**
     * Завершение активного рейса.
     */
    @PostMapping("/trips/{tripId}/finish")
    public Trip finishTrip(@PathVariable Long tripId, @RequestBody Map<String, String> payload) {
        LocalDateTime endTime = LocalDateTime.parse(payload.get("endTime"));
        return fleetService.finishTrip(tripId, endTime);
    }

    /**
     * Проведение обязательного ТО после рейса (только ПН–ПТ с 08:00 до 17:00).
     */
    @PostMapping("/maintenance")
    public MaintenanceRecord performMaintenance(@RequestBody Map<String, String> payload) {
        Long vehicleId = Long.valueOf(payload.get("vehicleId"));
        String description = payload.getOrDefault("description", "Плановый осмотр, мойка и заправка");
        LocalDateTime time = LocalDateTime.parse(payload.get("time"));
        return fleetService.performMaintenance(vehicleId, description, time);
    }

    /**
     * Добавление нового водителя.
     */
    @PostMapping("/drivers")
    public Driver addDriver(@RequestBody Map<String, Object> payload) {
        String fullName = (String) payload.get("fullName");
        @SuppressWarnings("unchecked")
        List<String> rawCats = (List<String>) payload.get("categories");
        Set<VehicleCategory> categories = new HashSet<>();
        for (String c : rawCats) {
            categories.add(VehicleCategory.valueOf(c));
        }
        return driverRepo.save(new Driver(fullName, categories));
    }

    /**
     * Добавление нового автомобиля в автопарк.
     */
    @PostMapping("/vehicles")
    public Vehicle addVehicle(@RequestBody Map<String, String> payload) {
        String licensePlate = payload.get("licensePlate");
        String model = payload.get("model");
        VehicleCategory category = VehicleCategory.valueOf(payload.get("category"));
        return vehicleRepo.save(new Vehicle(licensePlate, model, category));
    }

    /**
     * Перехват бизнес-ошибок (например, нет нужной категории прав или ТО в нерабочее время)
     * и возврат понятного текста ошибки в веб-интерфейс.
     */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBusinessException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
