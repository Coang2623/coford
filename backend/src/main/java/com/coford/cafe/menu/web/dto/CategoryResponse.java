package com.coford.cafe.menu.web.dto;

import com.coford.cafe.menu.domain.Category;

/**
 * CategoryResponse - DTO TRẢ VỀ thông tin danh mục cho client (sẽ được tuần tự hóa thành JSON).
 *
 * <p>Khái niệm: Java record bất biến chứa đúng các trường muốn lộ ra ngoài. Tránh trả thẳng
 * entity {@link Category} để không lộ chi tiết DB và tránh lỗi lazy loading khi serialize.</p>
 *
 * <p>Từ khóa: Java record, DTO, response model, factory method, mapping.</p>
 */
public record CategoryResponse(Long id, String name, int sortOrder) {

    // Factory method (phương thức nhà máy) "from": chuyển một entity Category thành DTO CategoryResponse.
    // static -> gọi trực tiếp qua class: CategoryResponse.from(category). Đây là điểm tập trung logic mapping.
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSortOrder());
    }
}
