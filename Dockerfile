# syntax=docker/dockerfile:1.7
# ============================================================
# Универсальный Dockerfile для всех Spring Boot модулей RAG-платформы.
# Модуль выбирается через --build-arg MODULE=<имя>.
# ============================================================

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 1. Сначала только pom-файлы — кэш зависимостей Maven
COPY pom.xml .
COPY common/pom.xml common/
COPY user-service/pom.xml user-service/
COPY document-service/pom.xml document-service/
COPY embedding-service/pom.xml embedding-service/
COPY retrieval-service/pom.xml retrieval-service/
COPY generation-service/pom.xml generation-service/
COPY api-gateway/pom.xml api-gateway/

# Прогрев локального репозитория Maven (ошибки игнорируем — некоторые
# артефакты докачаются на этапе package)
RUN mvn -B -q -N install -DskipTests && \
    mvn -B -q dependency:go-offline -DskipTests || true

# 2. Исходники
COPY common/src common/src
COPY user-service/src user-service/src
COPY document-service/src document-service/src
COPY embedding-service/src embedding-service/src
COPY retrieval-service/src retrieval-service/src
COPY generation-service/src generation-service/src
COPY api-gateway/src api-gateway/src

# 3. Сборка выбранного модуля (+ его зависимостей, например common)
ARG MODULE
RUN test -n "$MODULE" || (echo "ERROR: --build-arg MODULE is required" && exit 1) && \
    mvn -B -q -pl ${MODULE} -am package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
ARG MODULE
LABEL org.opencontainers.image.title="rag/${MODULE}"
WORKDIR /app

# Непривилегированный пользователь
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/${MODULE}/target/*.jar /app/app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]