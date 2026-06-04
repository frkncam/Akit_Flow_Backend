# Mühür Backend

Sözleşme yönetimi ve onay akışı için mikroservis tabanlı backend. Spring Boot 3.5 (Java 21),
Spring Cloud (Eureka + Gateway), PostgreSQL, RabbitMQ, MinIO.

---

## Mimari

```
                       ┌─────────────────┐
   istemci  ─────────► │  api-gateway    │ :8080   (tek giriş noktası, JWT doğrulama, routing)
                       └────────┬────────┘
                                │ lb:// (Eureka service discovery)
        ┌───────────┬───────────┼───────────┬───────────┬───────────┐
        ▼           ▼           ▼           ▼           ▼           ▼
     auth      contract    template    signature   notification  workflow / audit
     :8081      :8083       :8086        :8087        :8082        :8088 / :8089
        └───────────┴───────────┴─────┬─────┴───────────┴───────────┘
                                       ▼
              PostgreSQL · RabbitMQ · MinIO · (Mailpit – mail)
                                       ▲
                              eureka-server :8761 (service registry)
```

### Servisler & portlar

| Servis | Port | DB? | Açıklama |
|---|---|---|---|
| eureka-server | 8761 | – | Service discovery (registry) |
| api-gateway | 8080 | – | API gateway, routing, JWT, Swagger toplama |
| auth | 8081 | ✓ | Kimlik/yetki, OAuth2 token (JWKS) |
| notification | 8082 | ✓ | Bildirim + e-posta (Mailpit) |
| contract | 8083 | ✓ | Sözleşme (PDF → MinIO) |
| template | 8086 | ✓ | Şablon yönetimi |
| signature | 8087 | ✓ | İmza (dosya → MinIO) |
| workflow | 8088 | ✓ | Onay akışı |
| audit | 8089 | ✓ | Denetim kaydı |

### Altyapı servisleri (docker-compose.yml)

| Servis | Port(lar) | UI |
|---|---|---|
| PostgreSQL | 5432 | – |
| RabbitMQ | 5672 (AMQP), 15672 | http://localhost:15672 (guest/guest) |
| Mailpit | 1025 (SMTP), 8025 | http://localhost:8025 |
| MinIO | 9000 (API), 9001 | http://localhost:9001 (minioadmin/minioadmin) |

---

## Ön Koşullar

- **Docker Desktop** (compose v2 ile)
- Kaynaktan çalıştıracaksan: **JDK 21** + Maven (5 serviste `mvnw` wrapper mevcut)

---

## Kurulum

```powershell
# 1) Ortam değişkenleri: örneği kopyala (gerçek şifreleri .env'e yaz, .env git-ignored)
Copy-Item .env.example .env
```

> `.env` olmadan da çalışır — compose dosyalarındaki varsayılanlar (`akitflow`/`akitflow`,
> `guest`/`guest`, `minioadmin`/`minioadmin`) devreye girer. Üretimde mutlaka değiştir.

---

## Çalıştırma — 2 Mod

### Mod 1 — Sadece altyapı (lokal geliştirme)

Altyapıyı (Postgres, RabbitMQ, Mailpit, MinIO) container'da kaldır, **servisleri IDE/Maven**
ile lokalde çalıştır. Geliştirme/debug için en pratik mod.

```powershell
# Altyapıyı başlat
docker compose up -d

# Durumu gör (hepsi healthy olmalı)
docker compose ps

# Sıra önemli: önce registry, sonra servisler. Her birini ayrı terminalde:
cd eureka-server ; ./mvnw spring-boot:run      # önce bu ayağa kalksın
cd auth          ; ./mvnw spring-boot:run
cd contract      ; ./mvnw spring-boot:run
# ... ihtiyacın olan servisler (mvnw olmayan serviste: mvn spring-boot:run)
```

Servisler `application.yaml`'daki varsayılanlarla (`localhost`) altyapıya bağlanır — ek ayar
gerekmez.

### Mod 2 — Her şey container'da (tam stack)

Önce image'ları build et, sonra altyapı + uygulamaları birlikte kaldır.

```powershell
# 1) 9 servisin image'ını build et (build context = repo KÖKÜ; -f ile Dockerfile seç)
$services = "eureka-server","api-gateway","auth","notification","contract",
            "template","signature","workflow","audit"
foreach ($s in $services) {
  docker build -f "$s/Dockerfile" -t "muhur/$s:local" .
}

# 2) Altyapı (docker-compose.yml) + uygulamalar (compose.prod.yml) birlikte
docker compose -f docker-compose.yml -f compose.prod.yml up -d

# 3) Tüm servisler healthy mi?
docker compose -f docker-compose.yml -f compose.prod.yml ps
```

> **Neden build context repo kökü?** `auth`, `contract` gibi 7 servis ortak `common`
> artifact'ına (`com.muhur:common:1.0.0`) bağlı. Multi-stage build, common'ı container
> içinde derleyebilmek için `common/` klasörünü de görmek zorunda — bu yüzden context kök (`.`),
> Dockerfile ise `-f <servis>/Dockerfile` ile seçilir. (eureka-server ve api-gateway common'a
> bağlı değil ama tutarlılık için aynı komut kullanılır.)

#### Tek servisi build et / yeniden çalıştır

```powershell
docker build -f contract/Dockerfile -t muhur/contract:local .
docker compose -f docker-compose.yml -f compose.prod.yml up -d contract
```

#### Durdurma

```powershell
docker compose -f docker-compose.yml -f compose.prod.yml down       # container'ları kaldır
docker compose -f docker-compose.yml -f compose.prod.yml down -v     # + volume'leri sil (DB sıfırlanır)
```

---

## Erişim Noktaları (Mod 2 ayaktayken)

| Ne | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Swagger UI (tüm servisler) | http://localhost:8080/swagger-ui.html |
| Eureka dashboard | http://localhost:8761 |
| RabbitMQ yönetim | http://localhost:15672 |
| Mailpit (giden mailler) | http://localhost:8025 |
| MinIO konsol | http://localhost:9001 |
| Servis health örneği | http://localhost:8083/actuator/health |

---

## Docker Image Tasarımı (kısa)

Her servis **multi-stage** build kullanır:

- **build aşaması** (`maven:3.9-eclipse-temurin-21`): JDK + Maven; common'lı servislerde önce
  `common install`, sonra servis `package`.
- **runtime aşaması** (`eclipse-temurin:21-jre-alpine`): sadece JRE + jar. Maven/kaynak final
  image'a girmez → küçük image.
- Non-root `app` kullanıcısı, `-XX:MaxRAMPercentage=75.0`, `/actuator/health` healthcheck,
  `exec java ...` ile graceful shutdown (SIGTERM doğrudan JVM'e ulaşır).

`compose.prod.yml` image adlarını `${REGISTRY:-muhur}/<servis>:${TAG:-local}` ile çözer.
Registry'ye (GHCR) geçince yalnızca bu iki değişken set edilir:

```powershell
$env:REGISTRY = "ghcr.io/<kullanici>" ; $env:TAG = "<git-sha>"
docker compose -f docker-compose.yml -f compose.prod.yml up -d
```

---

## CI

Her `push`/`pull_request`'te `.github/workflows/ci.yml` 9 servisi **matrix** ile paralel
derler + test eder (önce `common install`, sonra servis `verify`; context yükleyen
`*ApplicationTests` CI'da dışlanır). Detay: `Analiz/31 MAYIS/01-ci-github-actions.md`.

---

## Sorun Giderme

- **`COPY common/...` başarısız** → `docker build` komutunu repo **kökünden** ve `-f` ile
  çalıştırdığından emin ol (context kök olmalı).
- **DB'li servis `healthy` olmuyor** → Postgres ayakta mı? (`docker compose ps`). DB servisleri
  açılışta Liquibase migration çalıştırır; Postgres olmadan ayağa kalkamaz.
- **Servis Eureka'da görünmüyor** → eureka-server'ın önce healthy olmasını bekle; `compose.prod.yml`
  zaten `depends_on: eureka-server: condition: service_healthy` ile bunu sağlar.
- **Port çakışması** → ilgili portu kullanan başka süreç var; durdur ya da compose port eşlemesini değiştir.
