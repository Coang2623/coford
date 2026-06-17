package com.coford.cafe.payment.domain;

import com.coford.cafe.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Thuc the (entity) luu mot lan thanh toan cho mot don hang.
 *
 * <p>Day la mot JPA entity: moi doi tuong Payment se duoc anh xa (map) thanh
 * mot dong trong bang "payment" cua co so du lieu. Class ke thua {@link BaseEntity}
 * de dung lai cac truong dung chung (vi du: id, thoi gian tao/sua).</p>
 *
 * <p>Quan he voi don hang: moi don hang (order) chi co toi da MOT ban thanh toan,
 * nen ve mat logic day la quan he 1-1 (one-to-one). Rang buoc nay duoc bao dam
 * boi cot orderId duoc danh dau {@code unique = true} (xem ben duoi).</p>
 *
 * <p>Tu khoa (keywords): JPA, entity, @Entity, @Table, one-to-one, BigDecimal money.</p>
 */
@Entity // Danh dau day la mot JPA entity, se duoc Hibernate quan ly va map sang bang DB.
@Table(name = "payment") // Chi dinh ten bang trong DB la "payment".
public class Payment extends BaseEntity {

    // Khoa ngoai (foreign key) tro toi don hang (order).
    // unique = true: moi orderId chi xuat hien 1 lan -> dam bao quan he 1-1 voi don hang
    // (mot don chi co the duoc thanh toan mot lan, chong thanh toan trung o tang DB).
    // nullable = false: bat buoc phai co orderId.
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    // Phuong thuc thanh toan (CASH/CARD/TRANSFER).
    // @Enumerated(EnumType.STRING): luu enum duoi dang chuoi ten ("CASH"...) thay vi so thu tu (ordinal),
    // giup du lieu trong DB de doc va an toan khi them/bot/sap xep lai cac gia tri enum sau nay.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    // So tien thanh toan. Dung BigDecimal (khong dung double/float) de tinh tien chinh xac,
    // tranh sai so lam tron cua so thuc dau cham dong (floating point). Tu khoa: BigDecimal money.
    @Column(nullable = false)
    private BigDecimal amount;

    // Thoi diem thanh toan, mac dinh la thoi diem tao doi tuong (now()).
    // OffsetDateTime: thoi gian co kem mui gio (timezone offset).
    @Column(name = "paid_at", nullable = false)
    private OffsetDateTime paidAt = OffsetDateTime.now();

    // Constructor khong tham so, pham vi protected: JPA/Hibernate bat buoc phai co
    // de tao doi tuong khi doc du lieu tu DB. Khong dung de goi truc tiep trong code.
    protected Payment() {
    }

    // Constructor dung trong code nghiep vu de tao moi mot ban thanh toan.
    public Payment(Long orderId, PaymentMethod method, BigDecimal amount) {
        this.orderId = orderId;
        this.method = method;
        this.amount = amount;
    }

    // Cac phuong thuc getter chi de doc gia tri ra ben ngoai (read-only).
    // Khong co setter -> doi tuong Payment gan nhu bat bien (immutable) sau khi tao,
    // giup du lieu thanh toan an toan, khong bi sua nham.
    public Long getOrderId() {
        return orderId;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }
}
