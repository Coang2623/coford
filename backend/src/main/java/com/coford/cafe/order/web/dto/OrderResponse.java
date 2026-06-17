package com.coford.cafe.order.web.dto;

import com.coford.cafe.order.domain.OrderEntity;
import com.coford.cafe.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String tableNo,
        OrderStatus status,
        BigDecimal totalAmount,
        String note,
        boolean prepared,
        OffsetDateTime createdAt,
        List<OrderLineResponse> items) {

    public static OrderResponse from(OrderEntity o) {
        return new OrderResponse(
                o.getId(), o.getTableNo(), o.getStatus(), o.getTotalAmount(), o.getNote(),
                o.isPrepared(), o.getCreatedAt(),
                o.getItems().stream().map(OrderLineResponse::from).toList());
    }
}
