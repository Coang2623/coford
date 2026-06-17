package com.coford.cafe.kitchen.web;

import com.coford.cafe.kitchen.KitchenSseService;
import com.coford.cafe.order.service.OrderService;
import com.coford.cafe.order.web.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controller REST cho màn hình bếp (Kitchen) real-time.
 *
 * <p>Cung cấp 2 endpoint:
 * <ul>
 *   <li>{@code GET /api/kitchen/orders}: lấy danh sách đơn cần pha (snapshot hiện tại) -
 *       dùng khi mới mở trang để hiển thị ngay.</li>
 *   <li>{@code GET /api/kitchen/stream}: mở luồng SSE (Server-Sent Events) để nhận
 *       cập nhật real-time về sau, thay vì phải tự hỏi server liên tục (polling).</li>
 * </ul>
 *
 * <p>{@code @RestController}: mọi giá trị trả về được tự chuyển thành JSON ghi vào HTTP response.
 * {@code @RequestMapping("/api/kitchen")}: tiền tố (prefix) chung cho mọi đường dẫn trong class.
 * {@code @Tag}: nhóm các endpoint này lại trong tài liệu Swagger/OpenAPI.
 *
 * <p>Từ khóa: REST controller, Server-Sent Events, text/event-stream, SseEmitter, Swagger OpenAPI
 */
@RestController
@RequestMapping("/api/kitchen")
@Tag(name = "Kitchen", description = "Màn hình bếp real-time")
public class KitchenController {

    // Service nghiệp vụ đơn hàng (lấy danh sách đơn cần pha).
    private final OrderService orderService;
    // Service quản lý các kết nối SSE (cấp emitter cho client mới).
    private final KitchenSseService sseService;

    // Constructor injection: Spring tự tiêm 2 service này khi khởi tạo controller.
    public KitchenController(OrderService orderService, KitchenSseService sseService) {
        this.orderService = orderService;
        this.sseService = sseService;
    }

    /** Trả về "bảng bếp" (kitchen board): các đơn chưa pha xong và chưa bị hủy. */
    @GetMapping("/orders")
    @Operation(summary = "Đơn cần pha (chưa xong, chưa hủy)")
    public List<OrderResponse> board() {
        return orderService.kitchenBoard();
    }

    /**
     * Mở luồng SSE. {@code produces = text/event-stream} báo cho trình duyệt biết đây là
     * luồng sự kiện dài hơi (long-lived), giữ kết nối mở để server đẩy dữ liệu xuống dần.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Luồng SSE event đơn hàng cho màn bếp")
    public SseEmitter stream() {
        return sseService.subscribe();
    }
}
