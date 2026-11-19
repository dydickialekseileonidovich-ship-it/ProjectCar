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

    @BeforeEach
    void setUp() {
        driver = driverRepo.save(new Driver("Алексей Смирнов", Set.of(VehicleCategory.TRUCK)));
        truck = vehicleRepo.save(new Vehicle("T999TT", "MAN TGX", VehicleCategory.TRUCK));
        bus = vehicleRepo.save(new Vehicle("M555MM", "Mercedes Tourismo", VehicleCategory.BUS));
    }

    @Test
    void testCategoryMismatchThrowsException() {
        assertThrows(IllegalStateException.class, () ->
                fleetService.startTrip(driver.getId(), bus.getId(), LocalDateTime.now())
        );
    }

    @Test
    void testUnservicedVehicleCannotStartNewTrip() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 16, 8, 0);
        Trip trip = fleetService.startTrip(driver.getId(), truck.getId(), start);
        fleetService.finishTrip(trip.getId(), start.plusHours(5));

        assertThrows(IllegalStateException.class, () ->
                fleetService.startTrip(driver.getId(), truck.getId(), start.plusHours(6))
        );
    }

    @Test
    void testMaintenanceOutsideWorkingHoursThrowsException() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 16, 8, 0);
        Trip trip = fleetService.startTrip(driver.getId(), truck.getId(), start);
        fleetService.finishTrip(trip.getId(), start.plusHours(5));

        LocalDateTime eveningTime = LocalDateTime.of(2026, 11, 16, 19, 0);
        assertThrows(IllegalStateException.class, () ->
                fleetService.performMaintenance(truck.getId(), "Мойка", eveningTime)
        );

        LocalDateTime validTime = LocalDateTime.of(2026, 11, 16, 14, 0);
        assertDoesNotThrow(() ->
                fleetService.performMaintenance(truck.getId(), "Мойка", validTime)
        );
    }

    /**
     * Тест 4: Дозаправка автомобиля прямо во время активного рейса (IN_USE) разрешена в любое время
     * и сохраняется в список работ без прерывания рейса.
     */
    @Test
    void testRefuelDuringActiveTripAllowed() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 16, 20, 0);
        fleetService.startTrip(driver.getId(), truck.getId(), start);

        MaintenanceRecord record = assertDoesNotThrow(() ->
                fleetService.performMaintenance(truck.getId(), "Дозаправка 100л на трассе", start.plusHours(2))
        );
        assertTrue(record.getDescription().contains("Дозаправка"));
        assertEquals(VehicleStatus.IN_USE, truck.getStatus());
    }
}
