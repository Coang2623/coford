package com.coford.cafe.report.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Doanh thu theo ngay. */
public record DailyRevenue(LocalDate day, BigDecimal revenue, long orderCount) {
}
