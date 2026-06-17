package com.coford.cafe.order.domain;

/**
 * Trang thai (status) cua mot don hang - vong doi cua don di qua cac trang thai nay.
 *
 * <p>Enum nay duoc luu vao DB dang chuoi (xem {@code @Enumerated(EnumType.STRING)} trong
 * OrderEntity), nen ten cac gia tri ("NEW", "PAID", "CANCELLED") chinh la gia tri luu trong
 * cot status.</p>
 *
 * <p>Luong chuyen trang thai (state transition) tieu bieu:
 * NEW -> PAID (thanh toan) hoac NEW -> CANCELLED (huy). Don da PAID thi khong huy duoc nua
 * (kiem tra trong OrderService.cancel()).</p>
 *
 * Tu khoa: enum, order status, state machine, lifecycle
 */
public enum OrderStatus {
    NEW,        // moi tao, dang phuc vu (vua goi mon, chua thanh toan)
    PAID,       // da thanh toan (don hoan tat)
    CANCELLED   // da huy (don bi bo)
}
