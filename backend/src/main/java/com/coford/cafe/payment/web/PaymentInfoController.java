package com.coford.cafe.payment.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thông tin tài khoản nhận tiền để frontend sinh mã VietQR. */
@RestController
@RequestMapping("/api/payment")
@Tag(name = "Payment", description = "Thanh toán và hóa đơn")
public class PaymentInfoController {

    private final String bank;
    private final String accountNumber;
    private final String accountName;

    public PaymentInfoController(
            @Value("${coford.payment.bank}") String bank,
            @Value("${coford.payment.account-number}") String accountNumber,
            @Value("${coford.payment.account-name}") String accountName) {
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
    }

    public record QrInfo(String bank, String accountNumber, String accountName) {
    }

    @GetMapping("/qr-info")
    @Operation(summary = "Thông tin tài khoản nhận tiền (cho VietQR)")
    public QrInfo qrInfo() {
        return new QrInfo(bank, accountNumber, accountName);
    }
}
