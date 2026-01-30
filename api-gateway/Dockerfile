# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy parent pom and common module first
COPY pom.xml ./
COPY common/pom.xml ./common/
COPY common/src ./common/src

# Copy api-gateway module
COPY api-gateway/pom.xml ./api-gateway/
COPY api-gateway/src ./api-gateway/src

# Build the application
RUN mvn -f api-gateway/pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from build stage
COPY --from=build /app/api-gateway/target/*.jar app.jar

# Expose the port
EXPOSE 8060

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8060/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
