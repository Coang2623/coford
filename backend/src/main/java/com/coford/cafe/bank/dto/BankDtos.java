package com.coford.cafe.bank.dto;

import java.math.BigDecimal;
import java.util.List;

public final class BankDtos {

    private BankDtos() {
    }

    public record AccountView(String number, String name, String currency, BigDecimal balance) {
    }

    public record BalanceView(BigDecimal total, String currency, List<AccountView> accounts) {
    }

    public record TxnView(
            String date,
            String direction,        // IN / OUT
            BigDecimal amount,
            String description,
            String counterparty,
            String bank,
            String refNo) {
    }

    public record BankStatus(boolean loggedIn, String username) {
    }

    public record LoginResult(boolean success, String message, String customerName) {
    }

    /** Một dòng đối soát: giao dịch tiền vào + đơn khớp (nếu có). */
    public record ReconcileRow(
            TxnView txn,
            Long matchedOrderId,
            String matchedTable,
            boolean matchedAlreadyPaid) {
    }
}
