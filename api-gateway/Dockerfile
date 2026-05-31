# Konum: api-gateway/Dockerfile
# ---- build aşaması: JDK + Maven (ağır), jar'ı üretir ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 1) Önce SADECE pom → bağımlılıklar ayrı katmanda cache'lenir.
COPY api-gateway/pom.xml api-gateway/pom.xml
RUN mvn -B -ntp -f api-gateway/pom.xml dependency:go-offline

# 2) Kaynağı kopyala ve paketle (testleri atla)
COPY api-gateway/src api-gateway/src
RUN mvn -B -ntp -f api-gateway/pom.xml package -DskipTests

# ---- runtime aşaması: SADECE JRE (hafif) ----
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL org.opencontainers.image.source=https://github.com/frkncam/Akit_Flow_Backend

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /workspace/api-gateway/target/*.jar app.jar
USER app

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=10 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
