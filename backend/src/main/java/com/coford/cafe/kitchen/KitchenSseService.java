package com.coford.cafe.kitchen;

import com.coford.cafe.common.config.KafkaConfig;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Cầu nối Kafka -> trình duyệt (real-time): nhận sự kiện từ Kafka rồi đẩy (push) xuống
 * tất cả client đang mở màn hình bếp thông qua SSE (Server-Sent Events).
 *
 * <p><b>Luồng dữ liệu tổng quát:</b>
 * <pre>
 *   Tạo/pha đơn  ->  KitchenEventPublisher gửi JSON lên topic Kafka
 *                ->  @KafkaListener ở đây nhận JSON đó (Kafka consumer)
 *                ->  broadcast() đẩy JSON xuống mọi trình duyệt qua SseEmitter
 *                ->  JavaScript trên trang bếp tự cập nhật giao diện.
 * </pre>
 *
 * <p><b>Vì sao dùng SSE chứ không phải WebSocket?</b> Màn bếp chỉ cần luồng dữ liệu MỘT
 * CHIỀU từ server xuống client (server -> browser), không cần client gửi ngược lên.
 * SSE đúng cho nhu cầu này: chạy trên HTTP thường, tự động kết nối lại (auto-reconnect)
 * phía trình duyệt, đơn giản hơn nhiều so với WebSocket (vốn là kênh hai chiều, nặng hơn).
 *
 * <p><b>Vì sao dùng {@link CopyOnWriteArrayList}?</b> Danh sách {@code emitters} bị truy cập
 * bởi nhiều luồng (thread) cùng lúc: luồng của {@code @KafkaListener} đang duyệt để broadcast,
 * trong khi luồng request HTTP khác lại đang thêm/xóa emitter. {@code CopyOnWriteArrayList}
 * an toàn luồng (thread-safe): mỗi lần ghi (add/remove) nó tạo bản sao mảng mới, nên việc
 * duyệt (iterate) không bao giờ bị {@code ConcurrentModificationException}.
 *
 * <p>Từ khóa: Kafka consumer, @KafkaListener, Server-Sent Events SseEmitter,
 * CopyOnWriteArrayList thread-safe, real-time push, one-way streaming
 */
@Service
public class KitchenSseService {

    private static final Logger log = LoggerFactory.getLogger(KitchenSseService.class);
    // Thời gian sống tối đa của một kết nối SSE trước khi server đóng (30 phút).
    private static final long TIMEOUT = 30 * 60 * 1000L; // 30 phút

    // Danh sách các kết nối SSE đang mở (mỗi tab/bếp = 1 SseEmitter). Thread-safe để duyệt + sửa song song.
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Đăng ký một kết nối SSE mới (gọi khi trình duyệt mở luồng /stream).
     * Trả về {@link SseEmitter} mà Spring MVC sẽ giữ mở để liên tục đẩy dữ liệu.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        // Cleanup (dọn dẹp): tự gỡ emitter khỏi danh sách khi kết nối kết thúc bình thường...
        emitter.onCompletion(() -> emitters.remove(emitter));
        // ...khi hết thời gian (timeout)...
        emitter.onTimeout(() -> emitters.remove(emitter));
        // ...hoặc khi có lỗi (vd: client đóng tab). Tránh giữ rác (memory leak) và gửi nhầm vào kết nối chết.
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    /**
     * Hàm lắng nghe Kafka (Kafka consumer). Spring Kafka tự gọi mỗi khi có message mới
     * trên topic ORDER_EVENTS, truyền vào chuỗi JSON đã được serialize sẵn.
     */
    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS)
    public void onKafkaEvent(String json) {
        log.info("[Kafka] nhận event đơn -> phát cho {} client bếp", emitters.size());
        broadcast(json);
    }

    /** Đẩy (gửi) cùng một chuỗi JSON xuống tất cả client đang kết nối. */
    private void broadcast(String json) {
        for (SseEmitter emitter : emitters) {
            try {
                // Gửi 1 sự kiện SSE với payload là chuỗi JSON.
                emitter.send(SseEmitter.event().data(json));
            } catch (IOException | IllegalStateException e) {
                // Kết nối đã chết/đóng -> gỡ ngay khỏi danh sách để lần sau không gửi nữa.
                emitters.remove(emitter);
            }
        }
    }
}
