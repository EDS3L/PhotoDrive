FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build/photodrive

COPY build.gradle settings.gradle gradlew ./
COPY gradle/ gradle/
RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

RUN java -Djarmode=layertools \
    -jar build/libs/*.jar extract --destination target/extracted


FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app/photodrive

RUN adduser -D -H -u 10001 -s /sbin/nologin appuser

COPY --from=builder /build/photodrive/target/extracted/dependencies/ ./
COPY --from=builder /build/photodrive/target/extracted/spring-boot-loader/ ./
COPY --from=builder /build/photodrive/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/photodrive/target/extracted/application/ ./

#
#RUN mkdir -p /app/photodrive/PHOTOGRAPHER@wp.pl/wesele_bartek011229@gmail.com_2025-12-09
#RUN mkdir -p /app/photodrive/portfolio
#RUN mkdir -p /app/photodrive/watermark

RUN chown -R appuser:appuser .

USER appuser

EXPOSE 8080

ENTRYPOINT ["java","org.springframework.boot.loader.launch.JarLauncher"]