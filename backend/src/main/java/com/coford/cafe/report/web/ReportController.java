package com.coford.cafe.report.web;

import com.coford.cafe.report.service.ReportService;
import com.coford.cafe.report.web.dto.DailyRevenue;
import com.coford.cafe.report.web.dto.TopItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Report", description = "Bao cao doanh thu")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/daily-revenue")
    @Operation(summary = "Doanh thu theo ngay")
    public List<DailyRevenue> dailyRevenue() {
        return service.dailyRevenue();
    }

    @GetMapping("/top-items")
    @Operation(summary = "Mon ban chay")
    public List<TopItem> topItems(@RequestParam(defaultValue = "5") int limit) {
        return service.topItems(limit);
    }
}
