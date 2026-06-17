package com.coford.cafe.kitchen;

import com.coford.cafe.common.config.KafkaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Bộ phát sự kiện đơn hàng lên Kafka (Kafka producer).
 *
 * <p>Vai trò: nhận một {@link KitchenEvent}, chuyển thành chuỗi JSON rồi gửi (publish/send)
 * vào topic Kafka. Phía bên kia (consumer) sẽ lắng nghe topic này để cập nhật màn bếp.
 *
 * <p>Triết lý "best-effort / fire-and-forget": việc gửi Kafka chỉ là phụ trợ (real-time UI).
 * Nếu Kafka lỗi/ngừng chạy, ta KHÔNG để exception lan ra ngoài làm hỏng nghiệp vụ chính
 * (tạo đơn vẫn phải thành công). Vì vậy toàn bộ logic gửi được bọc trong try/catch và
 * chỉ ghi log cảnh báo (warn) khi thất bại, rồi tiếp tục bình thường.
 *
 * <p>{@code @Component}: đánh dấu để Spring quản lý (Spring bean) và tự tiêm phụ thuộc
 * (dependency injection) {@code KafkaTemplate} cùng {@code ObjectMapper} qua constructor.
 *
 * <p>Từ khóa: Kafka producer, KafkaTemplate, fire-and-forget, best-effort, dependency injection
 */
@Component
public class KitchenEventPublisher {

    // Logger (SLF4J) dùng để ghi log cảnh báo khi gửi Kafka thất bại.
    private static final Logger log = LoggerFactory.getLogger(KitchenEventPublisher.class);

    // KafkaTemplate<key, value>: tiện ích của Spring Kafka để gửi message. Ở đây key & value đều là String (JSON).
    private final KafkaTemplate<String, String> kafka;
    // ObjectMapper (Jackson): chuyển object Java <-> chuỗi JSON (serialize / deserialize).
    private final ObjectMapper objectMapper;

    // Constructor injection: Spring tự truyền sẵn KafkaTemplate và ObjectMapper khi tạo bean.
    public KitchenEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    /**
     * Phát một sự kiện lên Kafka. An toàn để gọi trong luồng nghiệp vụ: nếu Kafka lỗi
     * cũng không ném exception ra ngoài (chỉ log warn).
     */
    public void publish(KitchenEvent event) {
        try {
            // 1) Serialize event thành chuỗi JSON để truyền qua mạng.
            String json = objectMapper.writeValueAsString(event);
            // 2) Gửi vào topic ORDER_EVENTS. Tham số 2 là "key" (orderId) -> các message cùng đơn
            //    sẽ vào cùng partition, giữ đúng thứ tự; tham số 3 là "value" (JSON).
            kafka.send(KafkaConfig.ORDER_EVENTS, String.valueOf(event.orderId()), json);
        } catch (Exception e) {
            // Nuốt lỗi (best-effort): chỉ ghi cảnh báo, không làm hỏng nghiệp vụ gọi tới.
            log.warn("Không publish được event Kafka: {}", e.getMessage());
        }
    }
}
