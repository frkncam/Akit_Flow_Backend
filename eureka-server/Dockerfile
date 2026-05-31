# Konum: eureka-server/Dockerfile
# ---- build aşaması: JDK + Maven (ağır), jar'ı üretir ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 1) Önce SADECE pom → bağımlılıklar ayrı katmanda cache'lenir.
#    Kod değişip pom değişmediğinde deps yeniden inmez (hız).
COPY eureka-server/pom.xml eureka-server/pom.xml
RUN mvn -B -ntp -f eureka-server/pom.xml dependency:go-offline

# 2) Kaynağı kopyala ve paketle (testleri burada atlıyoruz; testler CI'ın işi)
COPY eureka-server/src eureka-server/src
RUN mvn -B -ntp -f eureka-server/pom.xml package -DskipTests

# ---- runtime aşaması: SADECE JRE (hafif) ----
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL org.opencontainers.image.source=https://github.com/frkncam/Akit_Flow_Backend

# Güvenlik: root yerine ayrıcalıksız 'app' kullanıcısı
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

# build aşamasından SADECE jar'ı al (Maven, kaynak, .m2 final image'a girmez)
COPY --from=build /workspace/eureka-server/target/*.jar app.jar
USER app

EXPOSE 8761

# JVM container farkındalığı: heap'i konteynerin RAM yüzdesine göre ayarla
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]

# "ayakta" değil "hazır" mı? actuator health'e bak
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=10 \
  CMD wget -qO- http://localhost:8761/actuator/health || exit 1
