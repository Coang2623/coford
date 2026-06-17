package com.coford.cafe.report.web.dto;

import java.math.BigDecimal;

/** Mon ban chay. */
public record TopItem(String itemName, long totalQuantity, BigDecimal revenue) {
}
