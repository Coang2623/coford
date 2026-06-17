package com.coford.cafe.report.service;

import com.coford.cafe.report.web.dto.DailyRevenue;
import com.coford.cafe.report.web.dto.TopItem;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Bao cao tong hop. Dung JdbcTemplate + SQL aggregate (SUM/COUNT/GROUP BY)
 * vi dang nay viet bang SQL goc gon va ro hon JPQL.
 */
@Service
public class ReportService {

    private final JdbcTemplate jdbc;

    public ReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Doanh thu moi ngay (tu cac don da thanh toan). */
    public List<DailyRevenue> dailyRevenue() {
        String sql = """
                SELECT CAST(paid_at AS date) AS day,
                       SUM(amount)           AS revenue,
                       COUNT(*)              AS order_count
                FROM payment
                GROUP BY CAST(paid_at AS date)
                ORDER BY day DESC
                """;
        return jdbc.query(sql, (rs, i) -> new DailyRevenue(
                rs.getObject("day", LocalDate.class),
                rs.getBigDecimal("revenue"),
                rs.getLong("order_count")));
    }

    /** Top mon ban chay (theo so luong) trong cac don da thanh toan. */
    public List<TopItem> topItems(int limit) {
        String sql = """
                SELECT oi.item_name       AS item_name,
                       SUM(oi.quantity)   AS total_qty,
                       SUM(oi.line_total) AS revenue
                FROM order_item oi
                JOIN orders o ON o.id = oi.order_id
                WHERE o.status = 'PAID'
                GROUP BY oi.item_name
                ORDER BY total_qty DESC
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, i) -> new TopItem(
                rs.getString("item_name"),
                rs.getLong("total_qty"),
                rs.getBigDecimal("revenue")), limit);
    }
}
