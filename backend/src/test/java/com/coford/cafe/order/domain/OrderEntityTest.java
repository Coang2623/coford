package com.coford.cafe.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test thuan logic tinh tien cua don hang (khong can Spring/DB). */
class OrderEntityTest {

    @Test
    @DisplayName("addItem cong don dung tong tien")
    void addItemRecalculatesTotal() {
        OrderEntity order = new OrderEntity("B1", null);
        order.addItem(new OrderItem(1L, "Cà phê sữa", new BigDecimal("30000"), 2, null)); // 60000
        order.addItem(new OrderItem(4L, "Trà đào", new BigDecimal("40000"), 1, null));    // 40000

        assertThat(order.getTotalAmount()).isEqualByComparingTo("100000");
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems().get(0).getLineTotal()).isEqualByComparingTo("60000");
    }

    @Test
    @DisplayName("Don moi tao co trang thai NEW va tong tien 0")
    void newOrderDefaults() {
        OrderEntity order = new OrderEntity("B2", "ghi chu");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("0");
    }
}
