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

RUN mkdir -p /app/photodrive && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]