package com.coford.cafe.bank;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Lớp client gọi hệ thống ngân hàng lõi bên ngoài "CoreBank" qua HTTP.
 *
 * <p>Vai trò: là tầng "low-level" chỉ lo việc gửi request HTTP và trả về JSON thô
 * ({@link JsonNode}). Mọi xử lý nghiệp vụ (parse, đối soát...) nằm ở {@code BankService}.
 *
 * <p>Dùng {@link RestClient} - HTTP client kiểu mới (fluent/khai báo) của Spring 6+,
 * thay cho {@code RestTemplate} cũ. Các API của CoreBank chủ yếu là POST, body JSON.
 * Server CoreBank tự giữ phiên (session) sau khi đăng nhập; nếu phiên hết hạn, ta thử
 * đăng nhập lại (xử lý ở {@code BankService.withLogin}).
 *
 * <p><b>Timeout:</b> đặt connectTimeout 3s và readTimeout 15s để tránh treo vô hạn khi
 * CoreBank chậm hoặc không phản hồi - hệ thống ngoài không bao giờ được tin là luôn nhanh.
 *
 * <p><b>onStatus no-op:</b> mặc định RestClient sẽ ném exception khi gặp mã lỗi HTTP
 * (4xx/5xx). Nhưng CoreBank trả về JSON có ý nghĩa NGAY CẢ KHI lỗi (vd {success:false,
 * message:...}). Ta đăng ký một handler rỗng {@code (req, res) -> { }} để "tắt" hành vi
 * ném exception đó, nhờ vậy luôn đọc được body JSON và tự xử lý theo trường {@code success}.
 *
 * <p>Từ khóa: Spring RestClient, HTTP client, external API integration, timeout,
 * onStatus no-op, JsonNode
 */
@Component
public class CoreBankClient {

    // HTTP client đã cấu hình sẵn base-url + timeout.
    private final RestClient http;
    // Credentials cấu hình sẵn trong application.yml, dùng để tự đăng nhập lại khi rớt phiên.
    private final String username;
    private final String password;

    // @Value("${...:}"): đọc cấu hình; dấu ":" cuối nghĩa là mặc định rỗng nếu không cấu hình.
    public CoreBankClient(
            @Value("${coford.corebank.base-url}") String baseUrl,
            @Value("${coford.corebank.username:}") String username,
            @Value("${coford.corebank.password:}") String password) {
        // Factory cấu hình timeout cho từng request HTTP.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);   // tối đa 3s để thiết lập kết nối TCP
        factory.setReadTimeout(15000);     // tối đa 15s chờ đọc phản hồi
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.username = username;
        this.password = password;
    }

    /** Kiểm tra trạng thái phiên đăng nhập (GET /status). */
    public JsonNode status() {
        return http.get().uri("/status")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { }) // no-op: vẫn đọc body khi lỗi
                .body(JsonNode.class);
    }

    /** Lấy số dư các tài khoản (POST /balance, body rỗng vì server dựa vào session). */
    public JsonNode balance() {
        return post("/balance", Map.of());
    }

    /** Lấy sao kê (transactions) của một tài khoản trong khoảng ngày fromDate..toDate. */
    public JsonNode transactions(String accountNumber, String fromDate, String toDate) {
        return post("/transactions", Map.of(
                "accountNumber", accountNumber, "fromDate", fromDate, "toDate", toDate));
    }

    /** Đăng nhập với username/password truyền vào. */
    public JsonNode login(String user, String pass) {
        return post("/login", Map.of("username", user, "password", pass));
    }

    /**
     * Đăng nhập bằng credentials cấu hình sẵn (dùng để TỰ KHÔI PHỤC phiên khi rớt session).
     * Trả về true nếu JSON phản hồi có {@code success=true}; false nếu chưa cấu hình credentials.
     */
    public boolean login() {
        if (username == null || username.isBlank()) return false;
        return login(username, password).path("success").asBoolean(false);
    }

    /** Hàm dùng chung để gửi POST kèm body JSON và trả về JsonNode. */
    private JsonNode post(String path, Object body) {
        // onStatus no-op: CoreBank trả JSON cả khi lỗi (400/404) -> luôn đọc được body.
        return http.post().uri(path).body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { })
                .body(JsonNode.class);
    }
}
