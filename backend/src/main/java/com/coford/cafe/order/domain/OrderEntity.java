package com.coford.cafe.order.domain;

import com.coford.cafe.shared.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Don hang (Order) trong he thong quan ly cafe.
 *
 * <p>Day la mot JPA Entity - mot doi tuong Java duoc anh xa (map) sang mot dong (row)
 * trong bang co so du lieu. Mot don hang dai dien cho 1 lan goi mon tai 1 ban, gom
 * nhieu dong mon ({@link OrderItem}).</p>
 *
 * <p>Vi sao ten class la "OrderEntity" chu khong phai "Order"? Vi tu "order" la tu khoa
 * (reserved keyword) trong SQL (vd: ORDER BY). De tranh xung dot, ta dat ten bang la
 * "orders" (xem {@code @Table}) va dat ten class la OrderEntity cho ro rang.</p>
 *
 * <p>Ke thua (extends) {@link BaseEntity} - lop cha chua cac truong dung chung nhu khoa
 * chinh id (primary key).</p>
 *
 * Tu khoa: JPA Entity, table mapping, reserved keyword, primary key, domain model, aggregate root
 */
@Entity // Danh dau day la 1 thuc the JPA -> Hibernate se quan ly va luu vao DB
@Table(name = "orders") // Anh xa class nay toi bang ten "orders" (khong dung "order" vi trung tu khoa SQL)
public class OrderEntity extends BaseEntity {

    // So ban (table number). NOT NULL: bat buoc phai co.
    @Column(name = "table_no", nullable = false)
    private String tableNo;

    // Trang thai don. @Enumerated(EnumType.STRING): luu ten enum dang chuoi ("NEW", "PAID"...)
    // thay vi luu so thu tu (ordinal). Luu chuoi an toan hon: neu sau nay them/sap xep lai
    // gia tri enum, du lieu cu trong DB van dung nghia (khong bi lech index).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.NEW; // Mac dinh la NEW khi vua tao

    // Tong tien cua don (total amount). Dung BigDecimal cho tien te de tranh sai so lam tron
    // cua kieu double/float. Khoi tao = 0, se duoc tinh lai boi recalcTotal().
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Ghi chu cho don (optional - co the null).
    @Column
    private String note;

    // Co (flag) danh dau bep da pha che xong don nay chua. Dung cho man hinh bep (kitchen board).
    @Column(nullable = false)
    private boolean prepared = false;

    // Thoi diem tao. updatable = false: sau khi insert thi cot nay khong bao gio bi update.
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Thoi diem cap nhat gan nhat. Duoc tu dong cap nhat boi @PreUpdate (xem onUpdate()).
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Quan he 1-nhieu (one-to-many): 1 don hang co nhieu dong mon (OrderItem).
    // - mappedBy = "order": phia OrderItem moi la ben "so huu" (owning side) quan he qua cot khoa
    //   ngoai order_id. Ben nay (OrderEntity) chi la phia nghich dao (inverse side), khong tao
    //   them cot/bang trung gian.
    // - cascade = CascadeType.ALL: moi thao tac (save/delete...) tren OrderEntity se lan (cascade)
    //   xuong cac OrderItem -> luu/xoa don thi cac dong mon cung duoc luu/xoa theo.
    // - orphanRemoval = true: neu 1 OrderItem bi go khoi list "items" (mat cha) thi no se bi XOA
    //   khoi DB. Ket hop voi items.clear() trong updateItems() de xoa sach dong mon cu.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // Constructor khong tham so, protected: JPA/Hibernate yeu cau co de tao instance khi load tu DB.
    // Khong dung de goi truc tiep tu code nghiep vu.
    protected OrderEntity() {
    }

    // Constructor dung trong code: tao don moi voi so ban + ghi chu.
    public OrderEntity(String tableNo, String note) {
        this.tableNo = tableNo;
        this.note = note;
    }

    // @PreUpdate: callback vong doi (lifecycle callback) cua JPA. Hibernate goi method nay
    // TU DONG ngay truoc khi UPDATE dong nay xuong DB -> dam bao updatedAt luon moi nhat.
    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Them 1 dong mon vao don va tinh lai tong tien.
     * Gan order = this de thiet lap quan he 2 chieu (bidirectional) cho dung phia owning side.
     */
    public void addItem(OrderItem item) {
        item.setOrder(this); // gan cha cho con -> cot order_id duoc set khi luu
        this.items.add(item);
        recalcTotal(); // moi lan them mon -> cap nhat lai tong tien
    }

    /**
     * Tinh lai (recalculate) tong tien cua don = tong lineTotal cua tat ca dong mon.
     * Dung Stream: map moi item -> lineTotal, roi reduce (cong don) bat dau tu 0.
     * Goi lai moi khi danh sach mon thay doi de totalAmount luon dong bo voi cac dong mon.
     */
    public void recalcTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getTableNo() {
        return tableNo;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getNote() {
        return note;
    }

    public boolean isPrepared() {
        return prepared;
    }

    public void setPrepared(boolean prepared) {
        this.prepared = prepared;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
