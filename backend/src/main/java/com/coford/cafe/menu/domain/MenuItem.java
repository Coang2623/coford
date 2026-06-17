package com.coford.cafe.menu.domain;

import com.coford.cafe.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * MenuItem - Thực thể (entity) đại diện cho một MÓN trong menu (ví dụ: "Cà phê sữa đá").
 *
 * <p>Khái niệm: Là một JPA Entity ánh xạ tới bảng "menu_item". Mỗi món thuộc về MỘT danh mục
 * ({@link Category}) -> quan hệ nhiều-một (many-to-one): nhiều món có thể cùng thuộc một danh mục.</p>
 *
 * <p>Từ khóa: JPA Entity, many-to-one relationship, foreign key, lazy loading,
 * BigDecimal for money, audit timestamp.</p>
 */
@Entity
@Table(name = "menu_item")
public class MenuItem extends BaseEntity {

    // @ManyToOne: quan hệ NHIỀU-MỘT -> nhiều MenuItem trỏ về một Category.
    //   fetch = FetchType.LAZY: tải LƯỜI (lazy loading) -> Category KHÔNG được nạp ngay khi
    //     load MenuItem; chỉ truy vấn DB lấy Category khi thực sự gọi getCategory().
    //     Mục đích: tránh nạp dữ liệu thừa, tối ưu hiệu năng (performance).
    //   optional = false: bắt buộc phải có Category (không được null).
    // @JoinColumn: chỉ định cột KHÓA NGOẠI (foreign key) "category_id" trong bảng menu_item,
    //   trỏ tới khóa chính (id) của bảng category. nullable = false -> bắt buộc.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    // @Column không tham số: cột "description" cho phép NULL (mô tả là tùy chọn).
    @Column
    private String description;

    // price: dùng BigDecimal (không dùng double/float) để biểu diễn TIỀN tệ chính xác,
    // tránh sai số làm tròn của số dấu phẩy động (floating point rounding error).
    @Column(nullable = false)
    private BigDecimal price;

    // available: món có đang được bán hay không; mặc định true (đang bán).
    @Column(nullable = false)
    private boolean available = true;

    // createdAt: thời điểm tạo bản ghi (audit field).
    //   updatable = false -> cột này KHÔNG được cập nhật sau khi tạo (chỉ ghi một lần lúc insert).
    //   OffsetDateTime.now(): gán thời gian hiện tại (kèm offset múi giờ) làm giá trị mặc định.
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Constructor rỗng protected dành cho JPA/Hibernate (xem giải thích ở Category).
    protected MenuItem() {
    }

    // Constructor đầy đủ tham số để code nghiệp vụ tạo một MenuItem hợp lệ.
    public MenuItem(Category category, String name, String description, BigDecimal price, boolean available) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.available = available;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
