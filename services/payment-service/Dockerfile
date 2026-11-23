# ==============================================================================
# STAGE 1: Extract Spring Boot 4 layers from the packaged JAR
# ==============================================================================
FROM eclipse-temurin:21-jre-jammy AS extractor
WORKDIR /build

COPY target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

# ==============================================================================
# STAGE 2: Production Non-Root Runtime Image (JRE only)
# ==============================================================================
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system --gid 10001 appgroup && \
    useradd --system --uid 10001 --gid appgroup --shell /usr/sbin/nologin appuser && \
    chown -R appuser:appgroup /app

COPY --from=extractor --chown=appuser:appgroup /build/extracted/dependencies/ ./
COPY --from=extractor --chown=appuser:appgroup /build/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=appuser:appgroup /build/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=appuser:appgroup /build/extracted/application/ ./

USER 10001:10001

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8084

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8084/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
