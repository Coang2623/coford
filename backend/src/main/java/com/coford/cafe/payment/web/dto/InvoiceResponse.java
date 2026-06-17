package com.coford.cafe.payment.web.dto;

import com.coford.cafe.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Hoa don in cho khach. */
public record InvoiceResponse(
        Long orderId,
        String tableNo,
        List<InvoiceLine> lines,
        BigDecimal total,
        PaymentMethod method,
        OffsetDateTime paidAt) {
}
