package com.coford.cafe.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception (ngoại lệ) nghiệp vụ có kèm mã HTTP, để GlobalExceptionHandler đọc ra
 * và trả về đúng HTTP status cho client.
 *
 * Khái niệm:
 * - Kế thừa {@link RuntimeException} => đây là "unchecked exception": không bắt buộc
 *   khai báo throws hay try/catch. Code nghiệp vụ chỉ cần "throw" khi gặp lỗi, rất gọn.
 * - Trong Spring, khi controller/service ném exception này, lớp xử lý lỗi tập trung
 *   (@RestControllerAdvice) sẽ bắt và biến nó thành phản hồi JSON kèm mã status tương ứng.
 * - Các "factory method" tĩnh (notFound, badRequest) giúp tạo lỗi nhanh, đọc dễ hiểu.
 *
 * Từ khóa: custom exception, RuntimeException, unchecked exception, HttpStatus, factory method
 */
public class ApiException extends RuntimeException {
    // Mã HTTP đi kèm lỗi (vd: 404 NOT_FOUND, 400 BAD_REQUEST). "final" = gán 1 lần, không đổi.
    private final HttpStatus status;

    // Constructor (hàm khởi tạo): nhận mã status + thông điệp lỗi.
    public ApiException(HttpStatus status, String message) {
        super(message); // truyền message lên lớp cha RuntimeException để getMessage() trả về sau này
        this.status = status;
    }

    // Getter: cho phép GlobalExceptionHandler lấy ra mã HTTP để dựng phản hồi.
    public HttpStatus getStatus() {
        return status;
    }

    // Factory method tĩnh: tạo nhanh lỗi 404 (không tìm thấy tài nguyên).
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    // Factory method tĩnh: tạo nhanh lỗi 400 (dữ liệu yêu cầu không hợp lệ).
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
