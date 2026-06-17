package com.coford.cafe.payment.web.dto;

import java.math.BigDecimal;

public record InvoiceLine(String itemName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
}
