# ---- build stage: compile the jar. Tests run in CI (Backend CI gates the deploy), not on the VM.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# wrapper + build scripts first so the dependency download is cached until they change
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---- runtime stage: JRE only, non-root
FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home app
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health || exit 1
CMD ["java", "-jar", "app.jar"]
