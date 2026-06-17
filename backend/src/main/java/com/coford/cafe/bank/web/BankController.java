package com.coford.cafe.bank.web;

import com.coford.cafe.bank.BankService;
import com.coford.cafe.bank.dto.BankDtos.BalanceView;
import com.coford.cafe.bank.dto.BankDtos.BankStatus;
import com.coford.cafe.bank.dto.BankDtos.LoginResult;
import com.coford.cafe.bank.dto.BankDtos.ReconcileRow;
import com.coford.cafe.bank.dto.BankDtos.TxnView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank")
@Tag(name = "Bank", description = "Tích hợp CoreBank: số dư, sao kê, đối soát chuyển khoản")
public class BankController {

    private final BankService service;

    public BankController(BankService service) {
        this.service = service;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    @GetMapping("/status")
    @Operation(summary = "Trạng thái đăng nhập CoreBank")
    public BankStatus status() {
        return service.status();
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập CoreBank")
    public LoginResult login(@Valid @RequestBody LoginRequest req) {
        return service.login(req.username(), req.password());
    }

    @GetMapping("/balance")
    @Operation(summary = "Số dư tài khoản (CoreBank)")
    public BalanceView balance() {
        return service.balance();
    }

    @GetMapping("/transactions")
    @Operation(summary = "Sao kê N ngày gần nhất")
    public List<TxnView> transactions(@RequestParam(defaultValue = "30") int days) {
        return service.transactions(days);
    }

    @GetMapping("/reconcile")
    @Operation(summary = "Đối soát: giao dịch tiền vào + đơn khớp")
    public List<ReconcileRow> reconcile(@RequestParam(defaultValue = "30") int days) {
        return service.reconcile(days);
    }
}
