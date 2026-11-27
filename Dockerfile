FROM maven:3.6.3-jdk-8@sha256:ac624d39dd0b7bed827d4b0d1bbaeb457012e0cba0fb6c53ce34b00bed16ec08 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
COPY openapi.yaml .
COPY reference reference
RUN java -version && mvn -version \
    && mvn -B -Dtest=PricingTest,ApiErrorMappingTest,ShopConfigurationTest,InternalDiagnosticsTest verify

FROM build AS verification
CMD ["mvn", "-B", "verify"]

FROM eclipse-temurin:8-jre-jammy AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --uid 10001 --create-home app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/coffee-shop-1.0.0.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --start-period=60s --retries=30 CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
