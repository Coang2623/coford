package com.coford.cafe.kitchen;

import com.coford.cafe.order.web.dto.OrderResponse;

/**
 * Sự kiện (event) đẩy lên màn hình bếp.
 *
 * <p>Đây là một DTO sự kiện (event DTO) bất biến (immutable) dùng trong kiến trúc
 * hướng sự kiện (event-driven). Khi có việc xảy ra với đơn hàng, ta tạo một
 * {@code KitchenEvent} rồi gửi đi (qua Kafka) để màn bếp cập nhật real-time.
 *
 * <p>Có 2 loại (type):
 * <ul>
 *   <li>{@code CREATED}: đơn mới được tạo -> kèm theo toàn bộ thông tin đơn ({@code order}).</li>
 *   <li>{@code PREPARED}: đơn đã pha xong -> chỉ cần kèm {@code orderId} để bếp gạch khỏi danh sách.</li>
 * </ul>
 *
 * <p>Vì là {@code record} nên Java tự sinh constructor, getter (dạng {@code type()}, {@code order()}...),
 * {@code equals}/{@code hashCode}/{@code toString}. Hai phương thức {@code static} bên dưới là
 * "factory method" giúp tạo event đúng kiểu một cách rõ ràng.
 *
 * <p>Từ khóa: event-driven, DTO event, immutable record, factory method
 */
public record KitchenEvent(String type, OrderResponse order, Long orderId) {

    /** Tạo event báo "đơn mới" (CREATED) - kèm cả nội dung đơn để bếp hiển thị ngay. */
    public static KitchenEvent created(OrderResponse order) {
        return new KitchenEvent("CREATED", order, order.id());
    }

    /** Tạo event báo "đã pha xong" (PREPARED) - chỉ cần orderId, không cần gửi lại cả đơn. */
    public static KitchenEvent prepared(Long orderId) {
        return new KitchenEvent("PREPARED", null, orderId);
    }
}
