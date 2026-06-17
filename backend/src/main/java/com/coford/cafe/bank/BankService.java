package com.coford.cafe.bank;

import com.coford.cafe.bank.dto.BankDtos.AccountView;
import com.coford.cafe.bank.dto.BankDtos.BalanceView;
import com.coford.cafe.bank.dto.BankDtos.BankStatus;
import com.coford.cafe.bank.dto.BankDtos.LoginResult;
import com.coford.cafe.bank.dto.BankDtos.ReconcileRow;
import com.coford.cafe.bank.dto.BankDtos.TxnView;
import com.coford.cafe.common.exception.ApiException;
import com.coford.cafe.order.domain.OrderEntity;
import com.coford.cafe.order.domain.OrderStatus;
import com.coford.cafe.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Service nghiệp vụ tích hợp ngân hàng (tầng "business logic" cho CoreBank).
 *
 * <p>Vai trò: gọi {@link CoreBankClient} để lấy JSON thô, rồi PARSE linh hoạt thành các
 * DTO sạch ({@code BalanceView}, {@code TxnView}...) cho tầng web dùng. Đồng thời thực hiện
 * nghiệp vụ ĐỐI SOÁT (reconciliation): khớp giao dịch tiền vào tài khoản với đơn hàng theo số tiền.
 *
 * <p><b>Parse linh hoạt với {@link JsonNode}:</b> thay vì map cứng vào class, ta đọc từng
 * trường qua {@code node.path("field")}. {@code path()} không bao giờ trả null (trả về
 * "missing node") nên tránh được {@code NullPointerException} khi CoreBank thiếu trường nào đó.
 *
 * <p><b>withLogin tự đăng nhập lại:</b> nếu lần gọi đầu trả {@code success=false} (có thể do
 * rớt phiên/session), service tự gọi {@code client.login()} rồi thử lại đúng một lần.
 *
 * <p>Từ khóa: external API integration, Jackson JsonNode, reconciliation, business service,
 * defensive parsing, session re-login
 */
@Service
public class BankService {

    // Định dạng ngày dd/MM/yyyy mà API CoreBank yêu cầu cho tham số sao kê.
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CoreBankClient client;        // client gọi HTTP tới CoreBank
    private final OrderRepository orderRepo;    // truy vấn đơn hàng để đối soát
    private final String configuredAccount;     // số tài khoản cấu hình sẵn (nếu có)

    public BankService(CoreBankClient client, OrderRepository orderRepo,
                       @Value("${coford.corebank.account-number:}") String configuredAccount) {
        this.client = client;
        this.orderRepo = orderRepo;
        this.configuredAccount = configuredAccount;
    }

    /** Lấy trạng thái đăng nhập CoreBank và map sang DTO {@code BankStatus}. */
    public BankStatus status() {
        JsonNode s = client.status();
        String user = s.path("username").isMissingNode() ? null : s.path("username").asText(null);
        return new BankStatus(s.path("loggedIn").asBoolean(false), user);
    }

    public LoginResult login(String username, String password) {
        JsonNode r = client.login(username, password);
        boolean ok = r.path("success").asBoolean(false);
        return new LoginResult(
                ok,
                r.path("message").asText(ok ? "Đăng nhập thành công" : "Đăng nhập thất bại"),
                r.path("data").path("customerName").asText(null));
    }

    public BalanceView balance() {
        JsonNode r = requireSuccess(withLogin(client::balance));
        JsonNode data = r.path("data");
        List<AccountView> accounts = new ArrayList<>();
        for (JsonNode a : data.path("accounts")) {
            accounts.add(new AccountView(
                    a.path("number").asText(),
                    a.path("name").asText(),
                    a.path("currency").asText("VND"),
                    bd(a.path("balance"))));
        }
        return new BalanceView(bd(data.path("totalBalance")), data.path("currencyEquivalent").asText("VND"), accounts);
    }

    public List<TxnView> transactions(int days) {
        String account = resolveAccount();
        String to = LocalDate.now().format(DMY);
        String from = LocalDate.now().minusDays(Math.max(1, days)).format(DMY);
        JsonNode r = requireSuccess(withLogin(() -> client.transactions(account, from, to)));
        List<TxnView> result = new ArrayList<>();
        for (JsonNode t : r.path("data")) {
            BigDecimal credit = bd(t.path("creditAmount"));
            BigDecimal debit = bd(t.path("debitAmount"));
            boolean in = credit.signum() > 0;
            result.add(new TxnView(
                    t.path("transactionDate").asText(),
                    in ? "IN" : "OUT",
                    in ? credit : debit,
                    t.path("description").asText(""),
                    t.path("beneficiaryName").asText(""),
                    t.path("beneficiaryBank").asText(""),
                    t.path("refNo").asText("")));
        }
        return result;
    }

    /** Đối soát: với mỗi giao dịch TIỀN VÀO, tìm đơn (NEW) có tổng tiền khớp để xác nhận đã nhận. */
    public List<ReconcileRow> reconcile(int days) {
        List<TxnView> incoming = transactions(days).stream().filter(t -> "IN".equals(t.direction())).toList();
        List<OrderEntity> openOrders = orderRepo.findByStatus(OrderStatus.NEW);
        Set<Long> used = new HashSet<>();
        List<ReconcileRow> rows = new ArrayList<>();
        for (TxnView txn : incoming) {
            OrderEntity match = openOrders.stream()
                    .filter(o -> !used.contains(o.getId()))
                    .filter(o -> o.getTotalAmount().compareTo(txn.amount()) == 0)
                    .findFirst().orElse(null);
            if (match != null) {
                used.add(match.getId());
                rows.add(new ReconcileRow(txn, match.getId(), match.getTableNo(), false));
            } else {
                rows.add(new ReconcileRow(txn, null, null, false));
            }
        }
        return rows;
    }

    private String resolveAccount() {
        if (configuredAccount != null && !configuredAccount.isBlank()) return configuredAccount;
        List<AccountView> accounts = balance().accounts();
        if (accounts.isEmpty()) throw new ApiException(HttpStatus.BAD_GATEWAY, "CoreBank: không có tài khoản nào");
        return accounts.get(0).number();
    }

    /** Gọi API; nếu rớt session thì thử login lại rồi gọi lần nữa. */
    private JsonNode withLogin(Supplier<JsonNode> call) {
        JsonNode r = call.get();
        if (!r.path("success").asBoolean(false) && client.login()) {
            r = call.get();
        }
        return r;
    }

    private JsonNode requireSuccess(JsonNode r) {
        if (!r.path("success").asBoolean(false)) {
            String msg = r.path("message").asText("CoreBank không phản hồi hợp lệ");
            throw new ApiException(HttpStatus.BAD_GATEWAY, "CoreBank: " + msg);
        }
        return r;
    }

    private BigDecimal bd(JsonNode node) {
        try {
            return new BigDecimal(node.asText("0").replace(",", "").trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
