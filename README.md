# Coford — Ứng dụng Order Cà phê (mạng nội bộ)

Ứng dụng order cà phê dùng trong mạng LAN của quán: nhân viên tạo đơn theo bàn, thu ngân
thanh toán & xuất hóa đơn, quản lý xem báo cáo doanh thu. Đây là **project học tập** bám sát
stack công nghệ trong JD (Spring Boot 3, PostgreSQL, Redis, Kafka, Docker, K8S...).

## Stack
- **Backend:** Spring Boot 3.4 (Java 21), Spring Data JPA, PostgreSQL, Flyway, OpenAPI 3
- **Frontend:** React + Vite (sẽ thêm ở bước sau)
- **Hạ tầng:** Docker Compose (PostgreSQL)

## Cấu trúc dự án (package-by-feature + layered)
Mỗi feature là một "bounded context" với các tầng tách bạch — dễ đọc, dễ tách microservices sau.
```
backend/src/main/java/com/coford/cafe/
  CofordApplication.java
  shared/        BaseEntity (lớp cha chung cho entity)
  common/
    config/      OpenApiConfig
    exception/   ApiException, GlobalExceptionHandler
  menu/   order/   payment/   report/
    domain/      Entity (JPA)
    repository/  Spring Data repository
    service/     Business logic (@Transactional)
    web/         Controller (REST)
    web/dto/     Request/Response records (mỗi DTO 1 file)
```
Chuẩn dự án: Maven wrapper (`mvnw`), profile `dev`/`prod`, `Dockerfile` multi-stage,
biến môi trường cho DB (`DB_URL/DB_USERNAME/DB_PASSWORD`), test (`mvn test`), `.gitignore` + git.

## Phase 3 — Cache & Idempotency
- **Caffeine** (local cache L1): cache menu (`@Cacheable` trên `MenuService`), tự `@CacheEvict` khi thêm/sửa/xóa món. Test: gọi menu 3 lần chỉ chạm DB 1 lần.
- **Redis** (distributed): lưu `Idempotency-Key` (TTL 24h) chống **tạo đơn / thu tiền trùng**. Client gửi header `Idempotency-Key`; gọi lại cùng key → trả kết quả cũ, không xử lý lại.
- Hạ tầng: thêm service `redis` trong `infra/docker-compose.yml` (cổng 6379).

## Phase 4 — Real-time bếp (Kafka + SSE) & CoreBank
- **Kafka** (docker `kafka`, KRaft, 9092): tạo đơn → publish `coford.order-events`; backend consume (`@KafkaListener`) → đẩy xuống trình duyệt qua **SSE** (`/api/kitchen/stream`). Màn **Bếp** (`/kitchen`) hiện đơn ngay, bấm "Hoàn thành" (`/orders/{id}/prepare`) → publish `PREPARED` → ẩn đơn.
- **Tích hợp CoreBank** (`http://localhost:2001/api`): `CoreBankClient` (RestClient) gọi `/balance`, `/transactions`, `/login`. Trang **Ngân hàng** (`/bank`, sidebar — desktop) hiện số dư + **đối soát chuyển khoản**: khớp giao dịch tiền vào với đơn theo số tiền → nút "Thu cho #đơn" xác nhận thanh toán. Cấu hình qua env `COREBANK_URL/USER/PASS/ACCOUNT`.

## Phase 5 — Đăng nhập & phân quyền (Keycloak)
- **Keycloak** (docker, :8081, admin `admin/admin`) realm `coford` tự import (`infra/keycloak/coford-realm.json`): client public `coford-web`, 2 vai trò **STAFF**/**MANAGER**, 2 user mẫu:
  - `thungan` / `123456` — STAFF (chỉ Tạo đơn, Đơn hàng, Bếp)
  - `quanly` / `123456` — MANAGER (thêm Ngân hàng, Báo cáo, Quản lý menu)
- **Backend** = OAuth2 Resource Server xác thực JWT; phân quyền theo vai trò (reports/bank + ghi menu = MANAGER; còn lại cần đăng nhập). SSE `/api/kitchen/stream` để công khai (EventSource không gửi được Bearer).
- **Frontend** đăng nhập OIDC (`keycloak-js`), gắn Bearer vào mọi request, ẩn menu theo vai trò, có chip user + nút đăng xuất.
- Lưu ý LAN/điện thoại: cần đặt `VITE_KEYCLOAK_URL` = IP máy và thêm redirect/webOrigins tương ứng trong realm.

## Phase 6 — Observability (Metrics)
- Backend: **Spring Boot Actuator + Micrometer** → `/actuator/health`, `/actuator/prometheus` (mở quyền trong SecurityConfig).
- **Prometheus** (docker :9090) scrape backend qua `host.docker.internal:8080`.
- **Grafana** (docker :3000, `admin/admin`) — datasource + dashboard "Coford — Tổng quan" tự provision: số đơn, lượt thanh toán, HTTP req/s theo status, JVM heap, thanh toán theo phương thức.
- Metric nghiệp vụ tự thêm: `coford_orders_total`, `coford_payments_total{method=...}` (lưu ý: tên kết thúc `_created` bị Prometheus dành riêng → đặt `coford.orders`).

## Thiết kế UI (Pencil)
Giao diện đã được vẽ trong `coford.pen` (mở bằng Pencil) — 5 màn hình tablet ngang 1280×800:
Tạo đơn · Đơn hàng · Thanh toán & Hóa đơn · Báo cáo · Quản lý menu.

> Lưu ý: phong cách frontend đã đổi vài lần (espresso → editorial → **iOS 26**, theo phản hồi).
> Hiện tại dùng **iOS 26**, có **responsive** (mobile: ẩn sidebar, dùng thanh tab dưới đáy; bảng → thẻ xếp dọc).
> Bản Pencil đã được **sync token sang iOS** (màu terracotta + bo góc lớn + Inter); cấu trúc một số chi tiết
> (thumbnail, segmented) vẫn theo mockup cũ — frontend React là nguồn chuẩn.

**Design tokens — iOS 26 (đang dùng trong frontend):**
- Màu: nền grouped `#F2F1ED`, surface `#FFFFFF`, ink `#1A1714`, muted `#6F6A63`, line `#E6E2DB`,
  **tint đất nung `#B0451F`** (accent DUY NHẤT), success `#3F6B53`, danger `#C0392B`
- Font: **SF Pro / Inter** (`-apple-system` → Inter fallback), tiêu đề đậm `font-bold tracking-tight`
- Cấu trúc: **thẻ nhóm bo góc lớn** (card 20px, control 12px), **segmented control** thay tab,
  chọn dạng tô tint (accent-soft), badge dạng **capsule**, nút bo tròn nền tint, khoảng cách thoáng

## Kiến trúc
Khởi đầu **monolith** (1 Spring Boot app), tách dần thành microservices + Kafka ở phase sau.

```
React (Vite)  ──REST──►  Spring Boot monolith  ──►  PostgreSQL
                          ├─ menu     (quản lý món/danh mục)
                          ├─ order    (tạo & quản lý đơn)
                          ├─ payment  (thanh toán, hóa đơn)
                          └─ report   (báo cáo doanh thu)
```

## Lộ trình (mapping JD)
| Phase | Nội dung | Công nghệ JD | Trạng thái |
|-------|----------|--------------|-----------|
| 1 | Backend monolith: menu, order, payment, report | Spring Boot 3, PostgreSQL, REST, OpenAPI 3, Flyway | ✅ |
| 2 | Frontend đầy đủ (5 màn) nối API | Pencil, React + Vite + Tailwind v4 | ✅ |
| 3 | Cache (Caffeine) & idempotency (Redis) | Redis, Caffeine, Spring Cache | ✅ |
| 4 | Real-time bếp (Kafka + SSE) + tích hợp CoreBank | Kafka, SSE, REST client | ✅ |
| 5 | Auth & phân quyền nhân viên | Keycloak, Spring Security, OAuth2/OIDC | ✅ |
| 6 | Observability (metrics) | Actuator, Micrometer, Prometheus, Grafana | ✅ |
| 7 | Đóng gói & deploy | Docker, K8S/Minikube | ⬜ |
| 8 | CI/CD | GitLab CI, SonarQube | ⬜ |

## Chạy nhanh (Phase 1)
```bash
# 1. Bật PostgreSQL
docker compose -f infra/docker-compose.yml up -d

# 2. Chạy backend (dùng Maven wrapper, không cần cài Maven)
cd backend && ./mvnw spring-boot:run        # mặc định profile dev

# 3. Chạy test
./mvnw test

# 4. Đóng gói Docker (tùy chọn)
docker build -t coford-backend ./backend

# Swagger UI:  http://localhost:8080/swagger-ui.html
# Adminer DB:  http://localhost:8090  (server=postgres, user=coford, pass=coford)
```

## Chạy frontend (Phase 2)
```bash
cd frontend
npm install        # lần đầu
npm run dev        # mở http://localhost:5173
```
Frontend gọi backend qua proxy `/api` (cấu hình trong `vite.config.js`) — cần backend chạy ở :8080.

**Frontend (React + Vite + Tailwind v4):** 5 màn hình nối API thật
- `/` Tạo đơn · `/orders` Đơn hàng · `/orders/:id/pay` Thanh toán & Hóa đơn · `/reports` Báo cáo · `/menu` Quản lý menu
- Cấu trúc: `src/api.js` (client), `src/components/` (Sidebar, AppShell, ui), `src/pages/` (5 màn), `src/lib/format.js`
- QA hình ảnh: `node qa.mjs` (Playwright) chụp ảnh các màn vào `.qa/`
