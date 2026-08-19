FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew --no-daemon dependencies
COPY src src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && groupadd --system flowops \
    && useradd --system --gid flowops --home-dir /app flowops \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/build/libs/flowops-backend-0.0.1-SNAPSHOT.jar app.jar
USER flowops
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
