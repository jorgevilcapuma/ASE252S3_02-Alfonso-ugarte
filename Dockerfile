# =====================================================================
# Backend Spring Boot - Portal Ugartino Digital
# =====================================================================

# Etapa 1: compilar con Maven
FROM maven:3.9-amazoncorretto-25-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# Etapa 2: ejecutar con Java
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
