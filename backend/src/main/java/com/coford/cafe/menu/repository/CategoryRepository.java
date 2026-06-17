package com.coford.cafe.menu.repository;

import com.coford.cafe.menu.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * CategoryRepository - Lớp truy cập dữ liệu (repository / DAO) cho entity {@link Category}.
 *
 * <p>Khái niệm: Đây là một interface của Spring Data JPA. Ta CHỈ khai báo interface, KHÔNG
 * cần viết code triển khai (implementation) - Spring tự động sinh (generate) lớp cài đặt lúc chạy.
 * Kế thừa {@link JpaRepository}&lt;Category, Long&gt; nghĩa là: entity quản lý là Category,
 * kiểu khóa chính (primary key) là Long. Nhờ đó có sẵn các method CRUD: save, findById,
 * findAll, deleteById, existsById...</p>
 *
 * <p>Không cần annotation @Repository: Spring Data tự nhận diện interface kế thừa JpaRepository
 * là một bean repository (được @Repository ngầm định).</p>
 *
 * <p>Từ khóa: Spring Data JPA, repository, JpaRepository, CRUD, derived query method.</p>
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Derived query method (truy vấn suy ra từ tên method): Spring Data tự phân tích tên method
    // để sinh câu SQL. "findAll...OrderBySortOrderAsc" -> lấy TẤT CẢ category, SẮP XẾP (ORDER BY)
    // theo trường sortOrder TĂNG DẦN (Asc = ascending). Không cần viết SQL thủ công.
    List<Category> findAllByOrderBySortOrderAsc();
}
