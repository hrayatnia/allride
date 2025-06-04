FROM gradle:8.5-jdk17 AS build

WORKDIR /app
COPY . .

# Build the application with shadowJar
RUN gradle shadowJar --no-daemon

FROM openjdk:17-slim

WORKDIR /app
COPY --from=build /app/build/libs/allride-all.jar ./app.jar

# Create a non-root user
RUN useradd -m appuser && chown -R appuser:appuser /app
USER appuser

# Expose both HTTP and gRPC ports
EXPOSE 8080
EXPOSE 50051

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
