package com.example.fleet;

import com.example.fleet.model.*;
import com.example.fleet.repository.*;
import com.example.fleet.service.FleetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные автотесты JUnit 5 для проверки бизнес-правил автопарка.
 * @Transactional откатывает изменения в БД после каждого теста, чтобы тесты были независимыми.
 */
@SpringBootTest
@Transactional
public class FleetServiceTest {

    @Autowired
    private FleetService fleetService;

    @Autowired
    private DriverRepository driverRepo;

    @Autowired
    private VehicleRepository vehicleRepo;

    private Driver driver;
    private Vehicle truck;
    private Vehicle bus;

    // Подготовка тестовых данных перед каждым тестом
    @BeforeEach
    void setUp() {
        driver = driverRepo.save(new Driver("Алексей Смирнов", Set.of(VehicleCategory.TRUCK)));
        truck = vehicleRepo.save(new Vehicle("T999TT", "MAN TGX", VehicleCategory.TRUCK));
        bus = vehicleRepo.save(new Vehicle("M555MM", "Mercedes Tourismo", VehicleCategory.BUS));
    }

    /**
     * Тест 1: Проверка запрета на выезд, если у водителя нет нужной категории прав (TRUCK != BUS).
     */
    @Test
    void testCategoryMismatchThrowsException() {
        assertThrows(IllegalStateException.class, () ->
                fleetService.startTrip(driver.getId(), bus.getId(), LocalDateTime.now())
        );
    }

    /**
     * Тест 2: Проверка запрета на повторный выезд автомобиля, который не прошел ТО после рейса.
     */
    @Test
    void testUnservicedVehicleCannotStartNewTrip() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 16, 8, 0);
        Trip trip = fleetService.startTrip(driver.getId(), truck.getId(), start);
        fleetService.finishTrip(trip.getId(), start.plusHours(5));

        // Попытка выехать без обслуживания должна выбросить ошибку IllegalStateException
        assertThrows(IllegalStateException.class, () ->
                fleetService.startTrip(driver.getId(), truck.getId(), start.plusHours(6))
        );
    }

    /**
     * Тест 3: Проверка расписания работы ТО (разрешено только в будни с 08:00 до 17:00).
     */
    @Test
    void testMaintenanceOutsideWorkingHoursThrowsException() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 16, 8, 0);
        Trip trip = fleetService.startTrip(driver.getId(), truck.getId(), start);
        fleetService.finishTrip(trip.getId(), start.plusHours(5));

        // Попытка обслуживания в 19:00 (нерабочее время после 17:00) -> ожидаем ошибку
        LocalDateTime eveningTime = LocalDateTime.of(2026, 11, 16, 19, 0);
        assertThrows(IllegalStateException.class, () ->
                fleetService.performMaintenance(truck.getId(), "Мойка", eveningTime)
        );

        // Обслуживание в 14:00 в понедельник (рабочее время) -> проходит без ошибок
        LocalDateTime validTime = LocalDateTime.of(2026, 11, 16, 14, 0);
        assertDoesNotThrow(() ->
                fleetService.performMaintenance(truck.getId(), "Мойка", validTime)
        );
    }
}
