package com.coford.cafe.menu.domain;

import com.coford.cafe.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Category - Thực thể (entity) đại diện cho một DANH MỤC món trong menu quán cafe
 * (ví dụ: "Cà phê", "Trà", "Bánh ngọt").
 *
 * <p>Khái niệm: Đây là một JPA Entity, tức là một class Java được ánh xạ (mapping)
 * tới một bảng trong database. Mỗi đối tượng (object) Category tương ứng với một dòng (row)
 * trong bảng "category". Class kế thừa (extends) {@link BaseEntity} để dùng chung
 * trường khóa chính id (primary key).</p>
 *
 * <p>Từ khóa: JPA Entity, persistence, ORM (Object-Relational Mapping),
 * entity mapping, primary key, inheritance.</p>
 */
// @Entity: đánh dấu class này là một entity của JPA -> Hibernate sẽ quản lý và ánh xạ nó vào DB.
@Entity
// @Table: chỉ định tên bảng (table) trong database mà entity này ánh xạ tới là "category".
// Nếu không khai báo, mặc định tên bảng = tên class.
@Table(name = "category")
public class Category extends BaseEntity {

    // @Column: ánh xạ thuộc tính (field) này tới một cột (column) trong bảng.
    // nullable = false -> cột NOT NULL (bắt buộc có giá trị).
    // unique = true   -> cột có ràng buộc UNIQUE (tên danh mục không được trùng).
    @Column(nullable = false, unique = true)
    private String name;

    // name = "sort_order": tên cột trong DB là "sort_order" (khác tên field "sortOrder").
    // sortOrder dùng để sắp xếp thứ tự hiển thị danh mục; mặc định = 0.
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    // Constructor không tham số (no-args constructor) cấp protected:
    // JPA/Hibernate BẮT BUỘC cần một constructor rỗng để tạo đối tượng khi đọc dữ liệu từ DB.
    // Để protected thay vì public nhằm hạn chế việc tạo Category rỗng từ code nghiệp vụ.
    protected Category() {
    }

    // Constructor dùng trong code nghiệp vụ (business code) để tạo Category hợp lệ với đủ dữ liệu.
    public Category(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
