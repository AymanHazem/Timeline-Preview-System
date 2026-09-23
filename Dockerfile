# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-25-noble AS builder
WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:25-jre-noble

RUN apt-get update \
    && apt-get install --yes --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar

RUN mkdir -p uploads outputs \
    && chown -R app:app /app

USER app

ENV PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -Djava.awt.headless=true"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
