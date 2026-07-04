# ---- Build stage ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
# Download dependencies first (layer cache)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create directories
RUN mkdir -p /app/uploads/products /app/backups /app/logs

COPY --from=build /app/target/*.jar app.jar

# Render injects PORT env; Spring reads ${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java \
  -Dserver.port=${PORT:-8080} \
  -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-postgresql} \
  -Djava.security.egd=file:/dev/./urandom \
  -jar app.jar"]
