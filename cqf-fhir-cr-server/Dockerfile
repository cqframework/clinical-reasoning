# Multi-stage build for cqf-fhir-cr-server.
#
# Stage 1: copy the bootJar produced by `./gradlew :cqf-fhir-cr-server:bootJar`
#          into a layered tree so subsequent rebuilds only invalidate changed layers.
# Stage 2: minimal JRE 17 runtime.
#
# Build:  docker build -t cqf-fhir-cr-server:dev -f cqf-fhir-cr-server/Dockerfile .
# Run:    docker run --rm -p 8080:8080 cqf-fhir-cr-server:dev
#
# Alternative: `./gradlew :cqf-fhir-cr-server:bootBuildImage` builds an OCI image via
# Spring Boot's Paketo buildpacks without touching this file.

FROM eclipse-temurin:17-jre AS extractor
WORKDIR /work
ARG JAR_FILE=cqf-fhir-cr-server/build/libs/cqf-fhir-cr-server-*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher

FROM eclipse-temurin:17-jre
LABEL org.opencds.cqf.module="cqf-fhir-cr-server"
WORKDIR /opt/app

# Layered for cache efficiency. Dependencies change far less often than application code.
COPY --from=extractor /work/app/dependencies/ ./
COPY --from=extractor /work/app/spring-boot-loader/ ./
COPY --from=extractor /work/app/snapshot-dependencies/ ./
COPY --from=extractor /work/app/application/ ./

EXPOSE 8080

# JVM tuning for containers; rely on JDK 17's default ergonomics for memory.
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
