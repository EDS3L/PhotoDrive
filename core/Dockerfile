FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build/photodrive

COPY build.gradle settings.gradle gradlew ./
COPY gradle/ gradle/
RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon


FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN adduser -D -H -u 10001 -s /sbin/nologin appuser

COPY --from=builder /build/photodrive/build/libs/*.jar app.jar

# Pre-create data and temp directories so that named Docker volumes mounted here
# inherit the correct ownership (UID 10001) on first creation. Without this,
# Docker creates the mount points as root and the app can't write uploads.
RUN mkdir -p /app/photodrive /app/photodrive-temp && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]