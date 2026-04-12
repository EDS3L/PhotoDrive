FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build/photodrive

COPY build.gradle settings.gradle gradlew ./
COPY gradle/ gradle/
RUN ./gradlew dependencies --no-daemon

COPY src ./src
#RUN ./gradlew bootJar --no-daemon
RUN ./gradlew test bootJar --no-daemon


FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app/photodrive

RUN adduser -D -H -u 10001 -s /sbin/nologin appuser

COPY --from=builder /build/photodrive/build/libs/*.jar app.jar

RUN chown -R appuser:appuser .

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]