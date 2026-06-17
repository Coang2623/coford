package com.coford.cafe.menu.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * CategoryRequest - DTO (Data Transfer Object) nhận dữ liệu TẠO/CẬP NHẬT danh mục từ client.
 *
 * <p>Khái niệm: đây là một Java record - kiểu dữ liệu BẤT BIẾN (immutable) ngắn gọn, tự sinh
 * constructor, getter (dạng name(), sortOrder()), equals/hashCode/toString. Dùng record cho DTO
 * giúp code gọn và an toàn. DTO tách biệt dữ liệu vào/ra khỏi entity trong DB.</p>
 *
 * <p>Từ khóa: Java record, DTO, Bean Validation, immutable.</p>
 */
public record CategoryRequest(
        // @NotBlank (Bean Validation): tên không được null và không được rỗng/chỉ toàn khoảng trắng.
        @NotBlank String name,
        // sortOrder: thứ tự sắp xếp; không ràng buộc validation đặc biệt.
        int sortOrder) {
}
