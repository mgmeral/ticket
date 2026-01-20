# Ticket Reservation API (Case Study)

Spring Boot 3.4.x + Java 21 + MySQL + Flyway + JPA.

Bu proje; event/performer/seance yönetimi, seance availability hesabı, TTL’li hold (rezervasyon), mock payment authorize
ve idempotent purchase akışını içerir.  
Ayrıca observability için Actuator + Prometheus + Grafana hazır gelir.

---

## İçerik

- [Teknolojiler](#teknolojiler)
- [Hızlı Başlangıç (Docker Compose)](#hızlı-başlangıç-docker-compose)
- [Swagger / OpenAPI](#swagger--openapi)
- [Observability (Actuator + Prometheus + Grafana)](#observability-actuator--prometheus--grafana)
- [Correlation ID](#correlation-id)
- [Ana Kavramlar](#ana-kavramlar)
- [Tipik Akış (Örnek)](#tipik-akış-örnek)
- [Temel Endpoint’ler (Özet)](#temel-endpointler-özet)
- [Testler](#testler)
- [Konfigürasyon](#konfigürasyon)
- [Üretim Notu: Payment authorized ama DB fail](#üretim-notu-payment-authorized-ama-db-fail)
- [Troubleshooting](#troubleshooting)

---

## Teknolojiler

- Java 21
- Spring Boot 3.4.x
- Spring Web / Validation
- Spring Data JPA (Hibernate)
- MySQL 8.x
- Flyway
- Actuator + Micrometer Prometheus registry
- Prometheus + Grafana (docker-compose)
- (Opsiyonel) Frontend (docker-compose içinde)

---

## Hızlı Başlangıç (Docker Compose)

```bash
docker compose up --build
```

### Servisler

- Backend API: http://localhost:8080
- Frontend: http://localhost:3000
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001  (admin / admin)
- MySQL: localhost:3306
    - db: `ticket`
    - user: `ticket` / pass: `ticket`
    - root pass: `root`

> Flyway migration’ları uygulama startup’ında otomatik çalışır.

---

## Swagger / OpenAPI

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

---

## Observability (Actuator + Prometheus + Grafana)

### Actuator

- Health: http://localhost:8080/actuator/health
- Prometheus metrics: http://localhost:8080/actuator/prometheus

### Prometheus

Prometheus `prometheus.yml` ile şu endpoint’i scrape eder:

- `app:8080/actuator/prometheus`

Prometheus UI:

- http://localhost:9090

Örnek query’ler:

- `process_cpu_usage`
- `jvm_memory_used_bytes`
- `http_server_requests_seconds_count`
- `http_server_requests_seconds_sum`

### Grafana

Grafana UI:

- http://localhost:3001 (admin/admin)

Prometheus datasource ekleme:

1) **Connections → Data sources → Add data source → Prometheus**
2) URL: `http://prometheus:9090`
   > ÖNEMLİ: host üzerinden `localhost:9090` değil, docker network adı `prometheus` kullanılmalı.
3) Save & Test

Dashboard:

- **Dashboards → Import** ile JVM/Micrometer/Spring Boot dashboard’ları import edebilirsin.
- Arama: `Spring Boot`, `Micrometer`, `JVM`

---

## Correlation ID

API `X-Correlation-Id` header’ını destekler.

- Client header gönderirse aynı değer response header’ına geri yazılır ve MDC’ye set edilir.
- Gönderilmezse server UUID üretir.

Örnek:

```bash
curl -H "X-Correlation-Id: test-123" http://localhost:8080/actuator/health
```

---

## Ana Kavramlar

### Capacity / Availability

Availability hesabı:  
`available = capacity - sold - activeHeld`

- `sold`: ilgili seance için SOLD purchase toplamı
- `activeHeld`: **HELD** olup `expiresAt > now` olan hold’ların toplamı

Endpoint:

- `GET /seances/{id}/availability`

### Hold (TTL = 5 dakika)

Hold, kapasiteyi geçici olarak rezerve eder.

- `POST /holds` **idempotent** çalışır (request body’de `idempotencyKey`)
- `DELETE /holds/{id}` release eder

#### Hold expiration behavior

- **Lazy-expire:** Bir hold okunurken veya işlenirken TTL geçmişse API hold’u **EXPIRED** olarak değerlendirebilir.
- **DB cleanup:** `HoldExpiryJob` (default 30s) ile expired HELD kayıtlar bulk update ile **EXPIRED** yapılır.

Config:

```yaml
holds:
  expiry-job:
    enabled: true
    fixed-delay-ms: 30000
```

### Payment (Mock)

- `POST /payments/authorize`
    - Payment kaydını AUTHORIZED olarak oluşturur (mock).

### Purchase (Idempotent)

- `POST /purchases` idempotent (request body’de `idempotencyKey`)
- Validasyonlar:
    - payment mevcut + AUTHORIZED
    - hold mevcut + HELD + expire olmamış
    - amount eşleşiyor (quantity * unitPrice)
- Başarılı olursa:
    - purchase SOLD yaratılır
    - hold CONSUMED yapılır

---

## Tipik Akış (Örnek)

1) Performer oluştur
2) Event oluştur
3) Event’e performer’ları bağla
4) Event için seance oluştur
5) Seance için hold oluştur (**idempotencyKey ver**)
6) Payment authorize et (**paymentRef ver**)
7) Purchase oluştur (**idempotencyKey ver**)

---

## Temel Endpoint’ler (Özet)

### Events

- POST `/events`
- GET `/events/{id}`
- PUT `/events/{id}`
- DELETE `/events/{id}`
- GET `/events` (filters + pagination)
- PUT `/events/{id}/performers`

### Performers

- POST `/performers`
- GET `/performers/{id}`
- PUT `/performers/{id}`
- DELETE `/performers/{id}`
- GET `/performers` (filters + pagination)

### Seances

- POST `/events/{eventId}/seances`
- GET `/seances/{id}`
- GET `/seances` (filters + pagination)
- GET `/seances/{id}/availability`

### Holds

- POST `/holds` (idempotent)
- GET `/holds/{holdId}`
- DELETE `/holds/{holdId}` (release) release/soft delete

### Payments

- POST `/payments/authorize`

### Purchases

- POST `/purchases` (idempotent)

---

## Testler

```bash
mvn test
```

JaCoCo:

- `target/site/jacoco/index.html`

---

## Konfigürasyon

Management / metrics:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

Hold expiry job:

```yaml
holds:
  expiry-job:
    enabled: true
    fixed-delay-ms: 30000
```

---

## Troubleshooting

- Grafana Prometheus’a bağlanamıyorsa datasource URL’si `http://prometheus:9090` olmalı.
- Uygulama DB’ye bağlanamıyorsa MySQL healthcheck bitene kadar bekle veya compose’u yeniden başlat:
  ```bash
  docker compose down -v
  docker compose up --build
  ```
- Swagger açılmıyorsa:
    - `http://localhost:8080/swagger-ui/index.html` kullan.

## Kısa Runbook (Sık Görülen Sorunlar)

### DB yavaş (DB slow)

**Belirtiler:** API yanıtları yavaşlar / timeout olur.  
**Kontrol:** `/actuator/health`, `docker compose ps`, MySQL container health durumu, Grafana’da latency panelleri.  
**Çözüm (Mitigation):** MySQL’i restart et (`docker compose restart mysql`), DB kaynaklarını artır (CPU/RAM), indeksleri
kontrol et.

### DNS / servis adı çözümleme sorunları (docker network / service discovery)

**Belirtiler:** `UnknownHostException: mysql/prometheus` veya Prometheus target DOWN.  
**Kontrol:** `docker compose ps` (tüm servisler çalışıyor mu), Grafana datasource URL.  
**Çözüm (Mitigation):** `docker compose down && docker compose up --build`, Grafana’dan Prometheus’a bağlanırken
`http://prometheus:9090` kullan (localhost değil).

### Payment timeout (ödeme servisi yavaş/timeout)

**Belirtiler:** `/payments/authorize` veya `/purchases` isteği timeout olur / 5xx döner.  
**Kontrol:** `X-Correlation-Id` ile log takibi, payment status kontrolü, purchase isteğinde authorize response’tan dönen
`paymentRef` kullanılıyor mu kontrolü.  
**Çözüm (Mitigation):** makul client timeout’ları tanımla, retry sadece idempotent çağrılarda yap, AUTHORIZED payment
olup purchase oluşmayan durumlar için reconciliation/job yaklaşımı ekle.

## Failure handling: Payment authorized ama DB commit failed

Gerçek hayatta payment authorize dış sistemde bir side-effect’tir. DB commit hatasında "payment authorized" kalıp "
purchase oluşmadı" durumu oluşabilir.

**Strateji**

- Purchase endpoint’i idempotent: aynı `idempotencyKey` ile retry edildiğinde duplicate yaratmadan aynı sonucu döner.
- Reconciliation job: Periyodik olarak `AUTHORIZED` payment olup karşılık gelen purchase kaydı olmayan kayıtları tespit
  eder.
    - Hold hâlâ valid ise purchase finalize retry edilebilir
    - Hold expired/released ise payment provider’da void/cancel tetiklenir (mock senaryoda sadece işaretlenir)
- Ek: Gerekirse outbox/event log ile retry-safe arka plan işlemci tasarlanabilir.
