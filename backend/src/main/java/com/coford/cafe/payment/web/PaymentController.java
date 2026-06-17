package com.coford.cafe.payment.web;

import com.coford.cafe.payment.service.PaymentService;
import com.coford.cafe.payment.web.dto.InvoiceResponse;
import com.coford.cafe.payment.web.dto.PayRequest;
import com.coford.cafe.payment.web.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST cho cac API thanh toan va hoa don cua mot don hang.
 *
 * <p>{@code @RestController}: moi gia tri tra ve tu cac phuong thuc se tu dong
 * duoc chuyen thanh JSON va ghi vao body cua HTTP response.</p>
 *
 * <p>{@code @RequestMapping("/api/orders/{orderId}")}: tien to (prefix) duong dan
 * cho moi endpoint trong class, kem bien duong dan {@code orderId}.</p>
 *
 * <p>{@code @Tag}: chu thich nhom API cho tai lieu Swagger/OpenAPI.</p>
 *
 * <p>Tu khoa (keywords): Spring MVC, REST controller, @RestController, @RequestMapping,
 * path variable, request body, request header.</p>
 */
@RestController
@RequestMapping("/api/orders/{orderId}")
@Tag(name = "Payment", description = "Thanh toán và hóa đơn")
public class PaymentController {

    private final PaymentService service;

    // Constructor injection: Spring tu tiem PaymentService vao.
    public PaymentController(PaymentService service) {
        this.service = service;
    }

    /**
     * POST /api/orders/{orderId}/pay - thanh toan don.
     *
     * <p>{@code @PathVariable} lay orderId tu URL. {@code @Valid @RequestBody} doc JSON
     * body thanh PayRequest va kiem tra rang buoc (validation). {@code @RequestHeader}
     * doc header "Idempotency-Key" (khong bat buoc) de chong xu ly trung khi client retry.</p>
     */
    @PostMapping("/pay")
    @Operation(summary = "Thanh toán đơn")
    public PaymentResponse pay(
            @PathVariable Long orderId,
            @Valid @RequestBody PayRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return service.pay(orderId, req, idempotencyKey);
    }

    /**
     * GET /api/orders/{orderId}/invoice - xem hoa don cua don da thanh toan.
     */
    @GetMapping("/invoice")
    @Operation(summary = "Xem hóa đơn đã thanh toán")
    public InvoiceResponse invoice(@PathVariable Long orderId) {
        return service.invoice(orderId);
    }
}
