package com.coford.cafe.order.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Yeu cau tao don: so ban + danh sach mon. */
public record CreateOrderRequest(
        @NotBlank String tableNo,
        String note,
        @NotEmpty @Valid List<OrderLineRequest> items) {
}
