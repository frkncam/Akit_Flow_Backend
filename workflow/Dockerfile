# Konum: workflow/Dockerfile
# ---- build aşaması ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 1) Önce pom'lar (common + servis) → dep cache katmanı
COPY common/pom.xml common/pom.xml
COPY workflow/pom.xml workflow/pom.xml

# 2) common'ı yerel ~/.m2'ye kur
COPY common/src common/src
RUN mvn -B -ntp -f common/pom.xml install -DskipTests

# 3) Servisi paketle
COPY workflow/src workflow/src
RUN mvn -B -ntp -f workflow/pom.xml package -DskipTests

# ---- runtime aşaması ----
FROM eclipse-temurin:21-jre-alpine AS runtime
LABEL org.opencontainers.image.source=https://github.com/frkncam/Akit_Flow_Backend
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/workflow/target/*.jar app.jar
USER app

EXPOSE 8088
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
  CMD wget -qO- http://localhost:8088/actuator/health || exit 1
