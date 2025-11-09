FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app/photodrive

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew

RUN ./gradlew build -x test --no-daemon --stacktrace

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app/photodrive

COPY --from=builder /app/photodrive/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]




#FROM eclipse-temurin:21-jdk-jammy AS builder
#
#WORKDIR /app/photodrive
#
#COPY gradlew .
#COPY gradle gradle
#COPY build.gradle .
#COPY settings.gradle .
#COPY src src
#
#RUN chmod +x ./gradlew
#
#RUN ./gradlew build -x test --no-daemon --stacktrace
#
#FROM eclipse-temurin:21-jre-jammy
#
#WORKDIR /app/photodrive
#
#COPY --from=builder /app/photodrive/build/libs/*.jar app.jar
#
#EXPOSE 8080
#
#ENTRYPOINT ["java", "-jar", "app.jar"]

