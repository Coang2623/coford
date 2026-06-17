package com.coford.cafe.order.web;

import com.coford.cafe.order.domain.OrderStatus;
import com.coford.cafe.order.service.OrderService;
import com.coford.cafe.order.web.dto.CreateOrderRequest;
import com.coford.cafe.order.web.dto.OrderResponse;
import com.coford.cafe.order.web.dto.UpdateOrderItemsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "Tao va quan ly don hang")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo đơn hàng mới")
    public OrderResponse create(
            @Valid @RequestBody CreateOrderRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return service.create(req, idempotencyKey);
    }

    @GetMapping
    @Operation(summary = "Danh sách đơn (loc theo trang thái)")
    public List<OrderResponse> list(@RequestParam(required = false) OrderStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết đơn")
    public OrderResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}/items")
    @Operation(summary = "Sửa món của đơn chưa thanh toán")
    public OrderResponse updateItems(@PathVariable Long id, @Valid @RequestBody UpdateOrderItemsRequest req) {
        return service.updateItems(id, req.items());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy đơn")
    public OrderResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @PostMapping("/{id}/prepare")
    @Operation(summary = "Đánh dấu đơn đã pha xong (màn bếp)")
    public OrderResponse prepare(@PathVariable Long id) {
        return service.prepare(id);
    }
}
