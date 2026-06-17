package com.coford.cafe.order.domain;

import com.coford.cafe.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Mot dong (line item) trong don hang - dai dien cho "1 loai mon x so luong" trong 1 don.
 *
 * <p>Diem quan trong: class nay luu SNAPSHOT (ban chup) ten mon + gia tai thoi diem dat hang
 * (itemName, unitPrice) thay vi chi tham chieu toi MenuItem. Vi sao?</p>
 * <ul>
 *   <li>Gia/ten mon trong menu co the thay doi theo thoi gian (vd: tang gia, doi ten).</li>
 *   <li>Nhung don hang DA TAO phai giu nguyen gia/ten luc khach goi, neu khong thi hoa don cu
 *       se bi sai khi xem lai. -> Luu snapshot giup du lieu lich su (historical data) chinh xac.</li>
 * </ul>
 *
 * Tu khoa: JPA Entity, line item, price snapshot, historical data, denormalization, many-to-one
 */
@Entity
@Table(name = "order_item")
public class OrderItem extends BaseEntity {

    // Quan he nhieu-1 (many-to-one): nhieu dong mon thuoc ve 1 don hang.
    // - Day la phia "so huu" (owning side) quan he -> tao cot khoa ngoai order_id.
    // - fetch = FetchType.LAZY: chi tai (load) OrderEntity cha khi thuc su truy cap (getOrder()),
    //   tranh tu dong JOIN/query don cha moi khi load item (toi uu hieu nang).
    // - optional = false: dong mon BAT BUOC thuoc ve 1 don (khoa ngoai khong duoc null).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false) // cot khoa ngoai (foreign key) tro ve bang orders
    private OrderEntity order;

    // ID cua mon trong menu (tham chieu MenuItem). Luu id de biet mon goc, nhung KHONG dung de
    // hien thi ten/gia (da co snapshot ben duoi).
    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    // SNAPSHOT ten mon tai thoi diem dat. Giu nguyen du menu doi ten ve sau.
    @Column(name = "item_name", nullable = false)
    private String itemName;

    // SNAPSHOT don gia (unit price) tai thoi diem dat. Giu nguyen du menu doi gia ve sau.
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    // So luong (quantity) khach goi cho dong nay.
    @Column(nullable = false)
    private int quantity;

    // Thanh tien cua dong = unitPrice * quantity. Luu san (denormalized) de khong phai tinh lai.
    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    // Ghi chu rieng cho dong mon (vd: "khong duong", "it da"). Co the null.
    @Column
    private String note;

    // Constructor rong cho JPA (bat buoc), khong dung trong code nghiep vu.
    protected OrderItem() {
    }

    // Constructor dung trong code: nhan thong tin mon (da chup snapshot tu MenuItem) va tu tinh lineTotal.
    public OrderItem(Long menuItemId, String itemName, BigDecimal unitPrice, int quantity, String note) {
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.note = note;
        // Tinh thanh tien ngay tai day = don gia * so luong (dung BigDecimal de chinh xac voi tien te).
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // package-private (khong public): chi cho OrderEntity.addItem() goi de thiet lap quan he 2 chieu.
    void setOrder(OrderEntity order) {
        this.order = order;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public String getNote() {
        return note;
    }
}
