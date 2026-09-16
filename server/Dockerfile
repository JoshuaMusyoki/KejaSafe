# KejaSafe API image, used for local Docker and for Cloud Run.
#
# The Gradle build needs the Android SDK (the :core module is multiplatform), so the
# jar is built on the host and only the artifact is copied in:
#
#   ./gradlew :server:buildFatJar
#   docker build -f server/Dockerfile -t kejasafe/server:1.0.0 .
#
# Build context is the repository root.
FROM eclipse-temurin:21-jre-jammy

ARG JAR_PATH=server/build/libs/server-all.jar

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 kejasafe

WORKDIR /app
COPY ${JAR_PATH} /app/kejasafe-server.jar

# Defaults suit `docker compose up`, where /data is a persistent volume. On Cloud Run the
# filesystem is ephemeral and instances are replaced freely, so a deployment overrides
# KEJASAFE_DB_URL with managed Postgres and mounts a Cloud Storage bucket over the uploads
# directory. Container memory is capped per instance, hence MaxRAMPercentage.
ENV PORT=8080 \
    KEJASAFE_HOST=0.0.0.0 \
    KEJASAFE_DB_URL="jdbc:h2:file:/data/db/kejasafe;DB_CLOSE_DELAY=-1;" \
    KEJASAFE_UPLOAD_DIR=/data/uploads \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

RUN mkdir -p /data/db /data/uploads && chown -R kejasafe:kejasafe /data /app

USER kejasafe
VOLUME ["/data"]
EXPOSE 8080

# Cloud Run ignores this and probes the port itself; it covers Docker and Compose runs.
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -fsS "http://127.0.0.1:${PORT}/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/kejasafe-server.jar"]
