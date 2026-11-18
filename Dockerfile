# Этап 1: Сборка проекта с помощью Maven и JDK 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Сначала копируем pom.xml и скачиваем зависимости для кэширования слоев
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код и собираем JAR-файл
COPY src ./src
RUN mvn clean package -DskipTests -B

# Этап 2: Финальный легковесный образ с JRE 17
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
