# Coford App (Flutter) — POS quán cà phê chạy offline

Ứng dụng order cà phê **chạy hoàn toàn offline, không cần mạng**, viết lại bằng Flutter từ
dự án Coford (Spring Boot + React). Dữ liệu lưu cục bộ bằng **SQLite (Drift)** — chạy được trên
**Web** (WASM) và **Android** (native). Giao diện theo tinh thần **iOS 26** (large title, thẻ bo
góc nhóm, segmented control, tab bar dưới) — **không dùng hiệu ứng liquid glass**.

## Tính năng
- **Bán hàng:** chọn bàn, lọc danh mục, thêm món vào giỏ, gửi đơn.
- **Đơn hàng:** danh sách, tìm kiếm, lọc theo trạng thái (Chờ TT / Đã TT / Đã hủy).
- **Chi tiết đơn:** sửa món (đơn NEW), hủy đơn, chuyển sang thanh toán.
- **Thanh toán & hóa đơn:** Tiền mặt / Thẻ / Chuyển khoản (kèm QR + số tài khoản tĩnh), xem hóa đơn.
- **Bếp:** tự cập nhật real-time trong app (không cần SSE), đánh dấu "Hoàn thành", cảnh báo đơn > 5 phút.
- **Báo cáo:** doanh thu theo ngày (biểu đồ cột), số đơn, TB/đơn, top món bán chạy — tự cập nhật.
- **Quản lý:** CRUD danh mục & món, bật/tắt bán, cấu hình tên quán + thông tin chuyển khoản.

> So với bản gốc: bỏ đăng nhập/phân quyền (mọi màn mở), bỏ tích hợp CoreBank/đối soát (cần mạng);
> giữ lại cấu hình QR chuyển khoản tĩnh. Snapshot giá/tên món khi đặt được giữ nguyên.

## Kiến trúc
- `lib/data/database.dart` — schema Drift (categories, menu_items, orders, order_items, payments, settings).
- `lib/data/repository.dart` — logic nghiệp vụ + stream phản ứng (Drift `.watch()`).
- `lib/data/seed.dart` — dữ liệu mẫu nạp lần đầu chạy.
- `lib/providers.dart` — Riverpod providers.
- `lib/theme/app_theme.dart`, `lib/widgets/ui.dart` — theme + bộ widget iOS 26.
- `lib/screens/*` — các màn hình.

## Chạy thử

### Web (Chrome)
```bash
flutter run -d chrome
```
> File `web/sqlite3.wasm` và `web/drift_worker.js` đã có sẵn (bắt buộc cho Drift trên web).

### Android
```bash
flutter run -d <device_id>      # xem `flutter devices`
# hoặc build APK:
flutter build apk --release
```

## Test & kiểm thử
```bash
flutter test          # unit test logic nghiệp vụ (tạo đơn, thanh toán, báo cáo)
flutter analyze       # phân tích tĩnh
```

### Kiểm thử UI tự động trên web (tùy chọn)
Thư mục `tool/` chứa script Playwright (chụp ảnh từng màn để kiểm tra trực quan):
```bash
flutter build web
node tool/serve.mjs          # phục vụ build/web tại http://127.0.0.1:8090
node tool/flow3.mjs          # chạy luồng tạo đơn → thanh toán → báo cáo, lưu ảnh vào tool/shots/
```

## Nếu sửa schema Drift
Sau khi đổi `database.dart`, sinh lại mã:
```bash
dart run build_runner build
```
