package com.coford.cafe.payment.web.dto;

import com.coford.cafe.payment.domain.Payment;
import com.coford.cafe.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        PaymentMethod method,
        BigDecimal amount,
        OffsetDateTime paidAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getMethod(), p.getAmount(), p.getPaidAt());
    }
}
