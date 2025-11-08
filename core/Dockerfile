FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app/photodrive

ARG DATASOURCE_PASSWORD
ARG DATASOURCE_URL
ARG DATASOURCE_USERNAME
ARG MAIL_PASSWORD
ARG MAIL_USERNAME
ARG TOKEN_SECRETKEY

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew

RUN ./gradlew build --no-daemon --stacktrace

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app/photodrive

COPY --from=builder /app/photodrive/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

