package com.coford.cafe.common.idempotency;

import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Phase 3 — Dịch vụ Idempotency (tính bất biến khi lặp request) dùng Redis.
 *
 * IDEMPOTENCY LÀ GÌ?
 * - Một thao tác là "idempotent" nếu thực hiện 1 lần hay NHIỀU lần cùng input đều cho
 *   CÙNG kết quả và CÙNG hiệu ứng phụ. Ví dụ: bấm "Thanh toán" 1 lần hay (do mạng chậm,
 *   user bấm lại, client tự retry) gửi 3 lần đều chỉ tạo 1 đơn / trừ tiền 1 lần.
 *
 * VÌ SAO QUAN TRỌNG TRONG THANH TOÁN?
 * - Mạng không đáng tin: client gửi request, server xử lý xong nhưng phản hồi bị mất =>
 *   client tưởng thất bại và GỬI LẠI. Nếu server xử lý lại => tạo đơn trùng, thu tiền 2 lần.
 * - Mục tiêu mong muốn là "exactly-once" (đúng một lần) ở góc nhìn nghiệp vụ.
 *
 * CÁCH LÀM Ở ĐÂY:
 * - Client gửi header "Idempotency-Key" (thường là 1 UUID, duy nhất cho mỗi hành động).
 * - Ta lưu vào Redis ánh xạ: key -> id kết quả (vd id đơn hàng), kèm TTL (thời gian sống).
 * - Lần sau cùng key đó tới: thấy key đã tồn tại => request này ĐÃ xử lý => trả lại id cũ,
 *   KHÔNG xử lý lại (chống tạo đơn / thu tiền lặp).
 * - Dùng setIfAbsent (tương đương lệnh Redis SETNX = "SET if Not eXists"): chỉ ghi được
 *   nếu key chưa tồn tại. Đây là thao tác NGUYÊN TỬ (atomic) ở phía Redis, đóng vai trò như
 *   một "khóa phân tán" (distributed lock) nhẹ để hạn chế RACE CONDITION khi nhiều request
 *   trùng key chạy song song (vd 2 server cùng nhận request gần như đồng thời).
 *
 * Lưu id (Long) thay vì cả object cho đơn giản & tránh rắc rối serialize; caller (bên gọi)
 * dùng id này để lấy lại bản ghi đầy đủ từ DB.
 *
 * LƯU Ý GIỚI HẠN: lớp này lưu kết quả SAU khi xử lý (saveResult) chứ chưa "đặt chỗ" key
 * ngay từ đầu, nên không phải khóa hoàn hảo; nó GIẢM tranh chấp chứ chưa đảm bảo tuyệt đối.
 *
 * Từ khóa: idempotency key, Redis, StringRedisTemplate, SETNX, setIfAbsent, distributed lock,
 *          exactly-once, race condition, TTL, retry-safe
 */
@Service
public class IdempotencyService {

    // StringRedisTemplate: tiện ích Spring để đọc/ghi Redis với key & value đều là String.
    private final StringRedisTemplate redis;
    // ttl (Time To Live): bản ghi idempotency tự hết hạn sau khoảng này để Redis không phình mãi.
    private final Duration ttl;

    // Constructor: Spring tự "tiêm" (dependency injection) StringRedisTemplate.
    // @Value: đọc cấu hình "coford.idempotency.ttl-hours"; nếu thiếu thì mặc định 24 (giờ).
    public IdempotencyService(StringRedisTemplate redis,
                              @Value("${coford.idempotency.ttl-hours:24}") long ttlHours) {
        this.redis = redis;
        this.ttl = Duration.ofHours(ttlHours); // đổi số giờ thành đối tượng Duration
    }

    /**
     * Tra cứu: trả về id kết quả đã lưu cho key (nếu request này TỪNG được xử lý).
     * Optional rỗng = chưa từng xử lý (hoặc key trống).
     */
    public Optional<Long> findResult(String scope, String key) {
        if (isBlank(key)) return Optional.empty();                 // không có key => không kiểm tra
        String value = redis.opsForValue().get(buildKey(scope, key)); // đọc GET từ Redis (null nếu chưa có)
        return Optional.ofNullable(value).map(Long::valueOf);      // có thì parse String -> Long
    }

    /**
     * Lưu id kết quả cho key. Dùng setIfAbsent (SETNX) để CHỈ ghi ở lần đầu tiên:
     * nếu key đã có, lệnh này không ghi đè => giữ nguyên kết quả lần đầu, giảm tranh chấp (race).
     * Kèm ttl để bản ghi tự dọn sau khi hết hạn.
     */
    public void saveResult(String scope, String key, Long resultId) {
        if (isBlank(key)) return; // không có key thì bỏ qua, coi như không bật idempotency
        redis.opsForValue().setIfAbsent(buildKey(scope, key), String.valueOf(resultId), ttl);
    }

    // Tạo key Redis có tiền tố + scope để tránh đụng độ giữa các loại hành động khác nhau.
    // Ví dụ: "idem:order:550e8400-...". scope giúp phân vùng (namespacing) key.
    private String buildKey(String scope, String key) {
        return "idem:" + scope + ":" + key;
    }

    // Tiện ích: kiểm tra chuỗi rỗng/null/chỉ chứa khoảng trắng.
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
