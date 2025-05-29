FROM gradle:8.5-jdk17 AS build

WORKDIR /app
COPY . .

# Build the application
RUN gradle installDist --no-daemon

FROM openjdk:17-slim

WORKDIR /app
COPY --from=build /app/build/install/allride ./

# Create a non-root user
RUN useradd -m appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENTRYPOINT ["bin/allride"] 