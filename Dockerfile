FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN mkdir -p /app/uploads/products

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Memory/thread footprint is tuned for small (512MB-1GB) instances like Render's free/starter
# plan, where an unconstrained JVM (default heap sizing + 200-thread Tomcat pool) can be
# OOM-killed (exit 137) even under very light single-admin traffic.
# JVM_MAX_HEAP and JAVA_OPTS can be overridden via env vars if the instance plan changes.
ENTRYPOINT ["sh","-c","java -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss256k -Xms128m -Xmx${JVM_MAX_HEAP:-384m} -XX:MaxMetaspaceSize=128m -XX:MaxDirectMemorySize=64m ${JAVA_OPTS:-} -Dserver.port=${PORT:-8080} -jar app.jar"]