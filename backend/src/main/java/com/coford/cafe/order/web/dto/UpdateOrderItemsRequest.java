package com.coford.cafe.order.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Cập nhật toàn bộ danh sách món của một đơn (chỉ áp dụng cho đơn chưa thanh toán). */
public record UpdateOrderItemsRequest(
        @NotEmpty @Valid List<OrderLineRequest> items) {
}
