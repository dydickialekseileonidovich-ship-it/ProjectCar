# UML и ER диаграммы проекта «Автопарк» (Вариант 4)

В этом документе собраны все диаграммы архитектуры, базы данных и бизнес-процессов системы.

---

## 1. Диаграмма классов (UML Class Diagram)
Отражает структуру Java-классов пакета `model` и `service`, их поля, методы и связи между ними.

```mermaid
classDiagram
    direction TB

    class VehicleCategory {
        <<enumeration>>
        MOTORCYCLE
        CAR
        TRUCK
        BUS
    }

    class VehicleStatus {
        <<enumeration>>
        AVAILABLE
        IN_USE
        NEEDS_MAINTENANCE
    }

    class Driver {
        -Long id
        -String fullName
        -Set~VehicleCategory~ categories
        -boolean busy
        +getId() Long
        +getFullName() String
        +isBusy() boolean
        +setBusy(boolean) void
        +getCategories() Set~VehicleCategory~
    }

    class Vehicle {
        -Long id
        -String licensePlate
        -String model
        -VehicleCategory category
        -VehicleStatus status
        +getId() Long
        +getLicensePlate() String
        +getModel() String
        +getCategory() VehicleCategory
        +getStatus() VehicleStatus
        +setStatus(VehicleStatus) void
    }

    class Trip {
        -Long id
        -Driver driver
        -Vehicle vehicle
        -LocalDateTime startTime
        -LocalDateTime endTime
        +getDurationHours() long
    }

    class MaintenanceRecord {
        -Long id
        -Vehicle vehicle
        -LocalDateTime timestamp
        -String description
    }

    class FleetService {
        +startTrip(Long driverId, Long vehicleId, LocalDateTime start) Trip
        +finishTrip(Long tripId, LocalDateTime end) Trip
        +performMaintenance(Long vehicleId, String desc, LocalDateTime time) MaintenanceRecord
        +getDriverWorkHoursReport() Map~String, Long~
        +getVehicleUsageReport() Map~String, Long~
    }

    Driver "1" --> "*" VehicleCategory : открытые категории
    Vehicle "*" --> "1" VehicleCategory : требует категорию
    Vehicle "*" --> "1" VehicleStatus : имеет статус
    Trip "*" --> "1" Driver : выполняет водитель
    Trip "*" --> "1" Vehicle : используется авто
    MaintenanceRecord "*" --> "1" Vehicle : обслуживает авто
    FleetService ..> Trip : управляет
    FleetService ..> MaintenanceRecord : регистрирует ТО
```

---

## 2. ER-диаграмма базы данных (Entity-Relationship Diagram)
Показывает структуру таблиц в реляционной БД **H2**, первичные (`PK`) и внешние (`FK`) ключи, которые автоматически создаёт Hibernate.

```mermaid
erDiagram
    DRIVERS {
        bigint id PK "Первичный ключ"
        varchar full_name "ФИО водителя"
        boolean busy "Занят ли в рейсе"
    }

    DRIVER_CATEGORIES {
        bigint driver_id FK "Ссылка на водителя"
        varchar categories "MOTORCYCLE | CAR | TRUCK | BUS"
    }

    VEHICLES {
        bigint id PK "Первичный ключ"
        varchar license_plate UK "Госномер (уникальный)"
        varchar model "Марка и модель"
        varchar category "Требуемая категория ТС"
        varchar status "AVAILABLE | IN_USE | NEEDS_MAINTENANCE"
    }

    TRIPS {
        bigint id PK "Первичный ключ"
        bigint driver_id FK "Водитель"
        bigint vehicle_id FK "Автомобиль"
        timestamp start_time "Время выезда"
        timestamp end_time "Время возврата"
    }

    MAINTENANCE_RECORDS {
        bigint id PK "Первичный ключ"
        bigint vehicle_id FK "Обслуженный автомобиль"
        timestamp timestamp "Дата и время ТО (будни 08:00-17:00)"
        varchar description "Описание работ"
    }

    DRIVERS ||--o{ DRIVER_CATEGORIES : "имеет категории"
    DRIVERS ||--o{ TRIPS : "совершает"
    VEHICLES ||--o{ TRIPS : "участвует в"
    VEHICLES ||--o{ MAINTENANCE_RECORDS : "проходит ТО"
```

---

## 3. Диаграмма состояний автомобиля (UML State Diagram)
Описывает жизненный цикл каждого автомобиля в автопарке и условия переходов между статусами.

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE : Регистрация нового авто в парке

    AVAILABLE --> IN_USE : startTrip()\n[Водитель свободен И имеет нужную категорию]

    IN_USE --> NEEDS_MAINTENANCE : finishTrip()\n[Рейс завершен, водитель освобожден]

    NEEDS_MAINTENANCE --> AVAILABLE : performMaintenance()\n[Только ПН-ПТ с 08:00 до 17:00]
```

---

## 4. Диаграмма последовательности (UML Sequence Diagram)
Показывает пошаговое взаимодействие компонентов при выезде в рейс, возврате и прохождении ТО.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Клиент / Диспетчер
    participant Service as FleetService
    participant DriverRepo as DriverRepository
    participant VehicleRepo as VehicleRepository
    participant TripRepo as TripRepository
    participant MaintRepo as MaintenanceRepository

    Note over Client,MaintRepo: 1. Выход в рейс (круглосуточно 24/7)
    Client->>Service: startTrip(driverId, vehicleId, startTime)
    Service->>DriverRepo: findById(driverId)
    Service->>VehicleRepo: findById(vehicleId)
    Service->>Service: Проверка: !driver.isBusy(), категория прав, status == AVAILABLE
    Service->>DriverRepo: save(driver [busy = true])
    Service->>VehicleRepo: save(vehicle [status = IN_USE])
    Service->>TripRepo: save(new Trip)
    Service-->>Client: Объект Trip (рейс начат)

    Note over Client,MaintRepo: 2. Возвращение из рейса
    Client->>Service: finishTrip(tripId, endTime)
    Service->>TripRepo: findById(tripId)
    Service->>DriverRepo: save(driver [busy = false])
    Service->>VehicleRepo: save(vehicle [status = NEEDS_MAINTENANCE])
    Service->>TripRepo: save(trip [endTime])
    Service-->>Client: Объект Trip (авто требует ТО)

    Note over Client,MaintRepo: 3. Прохождение ТО (только ПН-ПТ 08:00–17:00)
    Client->>Service: performMaintenance(vehicleId, "Мойка и заправка", time)
    Service->>Service: Проверка дня недели (ПН-ПТ) и часа (08:00..17:00)
    Service->>VehicleRepo: findById(vehicleId)
    Service->>VehicleRepo: save(vehicle [status = AVAILABLE])
    Service->>MaintRepo: save(new MaintenanceRecord)
    Service-->>Client: Запись о ТО (авто снова свободно)
```

---

## 5. Блок-схема алгоритма проверки выдачи авто и ТО (Flowchart)

```mermaid
flowchart TD
    Start(["Запрос на выезд: startTrip()"]) --> CheckDriver{"Водитель свободен?\n(!driver.isBusy)"}
    CheckDriver -- Нет --> Err1["Ошибка: Водитель уже в рейсе"]
    CheckDriver -- Да --> CheckCat{"Есть нужная категория?\n(categories.contains)"}
    CheckCat -- Нет --> Err2["Ошибка: Нет нужной категории прав"]
    CheckCat -- Да --> CheckVeh{"Статус авто AVAILABLE?"}
    CheckVeh -- Нет --> Err3["Ошибка: Авто в рейсе или ждет ТО"]
    CheckVeh -- Да --> DoStart["driver.busy = true\nvehicle.status = IN_USE\nСохранение Trip в БД"]
    DoStart --> Finish["Завершение рейса: finishTrip()\ndriver.busy = false\nvehicle.status = NEEDS_MAINTENANCE"]
    Finish --> MaintReq["Запрос на ТО: performMaintenance()"]
    MaintReq --> CheckTime{"Будний день (ПН-ПТ)\nи время 08:00–17:00?"}
    CheckTime -- Нет --> Err4["Ошибка: ТО работает только в будни 08:00–17:00"]
    CheckTime -- Да --> DoMaint(["vehicle.status = AVAILABLE\nСохранение MaintenanceRecord"])
```
