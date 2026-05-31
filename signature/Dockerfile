# Konum: signature/Dockerfile
# ---- build aşaması ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 1) Önce pom'lar (common + servis) → dep cache katmanı
COPY common/pom.xml common/pom.xml
COPY signature/pom.xml signature/pom.xml

# 2) common'ı yerel ~/.m2'ye kur
COPY common/src common/src
RUN mvn -B -ntp -f common/pom.xml install -DskipTests

# 3) Servisi paketle
COPY signature/src signature/src
RUN mvn -B -ntp -f signature/pom.xml package -DskipTests

# ---- runtime aşaması ----
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/signature/target/*.jar app.jar
USER app

EXPOSE 8087
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
  CMD wget -qO- http://localhost:8087/actuator/health || exit 1
