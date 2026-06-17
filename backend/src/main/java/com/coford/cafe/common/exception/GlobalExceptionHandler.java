package com.coford.cafe.common.exception;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Bắt lỗi TẬP TRUNG (centralized exception handling) cho toàn bộ REST API,
 * trả về JSON đồng nhất cho client thay vì để Spring trả ra lỗi mặc định lộn xộn.
 *
 * Khái niệm:
 * - @RestControllerAdvice: gộp 2 ý nghĩa @ControllerAdvice + @ResponseBody. Nó là một
 *   "advice" áp dụng cho TẤT CẢ các @RestController trong ứng dụng. Khi bất kỳ controller
 *   nào ném exception, Spring sẽ tìm trong lớp này một method có @ExceptionHandler khớp
 *   loại exception đó để xử lý. Nhờ vậy, logic xử lý lỗi nằm 1 chỗ (DRY), không lặp ở từng controller.
 * - @ExceptionHandler(X.class): đánh dấu method này chịu trách nhiệm xử lý exception loại X.
 *   Spring chọn handler "khớp cụ thể nhất" (vd: ApiException được ưu tiên hơn Exception chung).
 * - ResponseEntity: cho phép tự kiểm soát cả HTTP status + body trả về.
 *
 * Từ khóa: Spring exception handling, RestControllerAdvice, ExceptionHandler,
 *          centralized error handling, Bean Validation, ResponseEntity
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Xử lý lỗi nghiệp vụ tự định nghĩa: lấy đúng status đã gắn trong ApiException để trả về.
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    // Xử lý lỗi validate dữ liệu đầu vào (Bean Validation / Jakarta Validation).
    // MethodArgumentNotValidException được Spring ném khi tham số @RequestBody gắn @Valid
    // vi phạm ràng buộc (vd: @NotBlank, @Size, @Min...). Ở đây ta gom mọi lỗi field thành 1 chuỗi.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        // getBindingResult().getFieldErrors(): danh sách các lỗi theo từng field.
        // stream + map + reduce: ghép tất cả lỗi lại thành "field1: msg1; field2: msg2".
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError) // mỗi FieldError -> "tên field: thông điệp"
                .reduce((a, b) -> a + "; " + b) // nối các chuỗi lỗi, ngăn cách bằng "; "
                .orElse("Dữ liệu không hợp lệ"); // phòng trường hợp không có field error nào
        return build(HttpStatus.BAD_REQUEST, msg); // luôn trả 400 cho lỗi validate
    }

    // "Lưới an toàn" (fallback): bắt mọi exception còn lại chưa có handler riêng,
    // tránh để lộ stack trace thô ra client; trả về 500 Internal Server Error.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // Định dạng 1 lỗi field thành chuỗi dễ đọc: "tênField: thông điệp lỗi".
    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    // Hàm dùng chung để dựng body JSON lỗi theo cấu trúc thống nhất.
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        // LinkedHashMap: giữ NGUYÊN thứ tự chèn key => JSON trả về có thứ tự field ổn định, dễ đọc.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString()); // thời điểm xảy ra lỗi (chuẩn ISO-8601)
        body.put("status", status.value());                     // mã số HTTP, vd: 404
        body.put("error", status.getReasonPhrase());            // mô tả status, vd: "Not Found"
        body.put("message", message);                           // thông điệp chi tiết cho client
        // Trả ResponseEntity gồm đúng HTTP status + body JSON ở trên.
        return ResponseEntity.status(status).body(body);
    }
}
