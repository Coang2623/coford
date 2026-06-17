package com.coford.cafe.menu.repository;

import com.coford.cafe.menu.domain.MenuItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * MenuItemRepository - Lớp truy cập dữ liệu (repository) cho entity {@link MenuItem}.
 *
 * <p>Khái niệm: interface Spring Data JPA, kế thừa {@link JpaRepository}&lt;MenuItem, Long&gt;
 * để có sẵn các thao tác CRUD cơ bản. Bổ sung thêm các "derived query method" để lọc món.</p>
 *
 * <p>Từ khóa: Spring Data JPA, repository, derived query method, query derivation.</p>
 */
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    // Derived query method: "findByCategoryId" -> Spring sinh câu truy vấn
    // WHERE category.id = :categoryId, tức lấy tất cả món thuộc một danh mục cụ thể.
    List<MenuItem> findByCategoryId(Long categoryId);

    // Derived query method: "findByAvailableTrue" -> WHERE available = true,
    // tức chỉ lấy những món ĐANG BÁN (available = true). Hậu tố "True" là điều kiện cố định.
    List<MenuItem> findByAvailableTrue();
}
