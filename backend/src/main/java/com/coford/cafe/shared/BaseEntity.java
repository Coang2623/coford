package com.coford.cafe.shared;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Lớp cha chung (base class) cho các JPA entity: gom sẵn khóa chính (id) để tránh
 * lặp lại ở mọi entity. Các entity con chỉ cần "extends BaseEntity" là có sẵn trường id.
 *
 * Khái niệm:
 * - @MappedSuperclass: đánh dấu đây là lớp cha "ánh xạ chung" cho JPA. Bản thân lớp này
 *   KHÔNG tạo bảng riêng trong database; thay vào đó, các cột khai báo ở đây (vd: id) sẽ
 *   được "trộn" (inherit) vào bảng của từng entity con. Khác với @Entity (tạo bảng riêng)
 *   và khác với @Embeddable.
 * - @Id: chỉ định trường này là khóa chính (primary key) của bảng.
 * - @GeneratedValue(strategy = IDENTITY): khóa chính do DATABASE tự sinh, thường qua cột
 *   AUTO_INCREMENT (MySQL) / SERIAL-IDENTITY (PostgreSQL). Tức là giá trị id chỉ có sau khi
 *   bản ghi được INSERT vào DB. (Các chiến lược khác: SEQUENCE, TABLE, AUTO.)
 * - "abstract": không cho tạo trực tiếp BaseEntity; chỉ dùng để kế thừa (inheritance).
 * - Chỉ có getter (không có setter) cho id vì id do DB sinh ra, không nên gán tay.
 *
 * Từ khóa: JPA, MappedSuperclass, inheritance, primary key, Id, GeneratedValue,
 *          identity generation, auto increment
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id // khóa chính của entity
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB tự tăng & sinh giá trị khi INSERT
    private Long id;

    // Getter: cho phép đọc id. Không có setter vì id do database sinh tự động.
    public Long getId() {
        return id;
    }
}
