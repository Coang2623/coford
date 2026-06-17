package com.coford.cafe.order.web.dto;

import com.coford.cafe.order.domain.OrderItem;
import java.math.BigDecimal;

public record OrderLineResponse(
        Long id,
        Long menuItemId,
        String itemName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        String note) {

    public static OrderLineResponse from(OrderItem i) {
        return new OrderLineResponse(i.getId(), i.getMenuItemId(), i.getItemName(),
                i.getUnitPrice(), i.getQuantity(), i.getLineTotal(), i.getNote());
    }
}
