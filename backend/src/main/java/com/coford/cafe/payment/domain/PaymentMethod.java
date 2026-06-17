package com.coford.cafe.payment.domain;

/**
 * Kieu liet ke (enum) cac phuong thuc thanh toan ma quan cafe ho tro.
 *
 * <p>Enum la mot tap hop co dinh, huu han cac gia tri co ten. Dung enum thay vi
 * chuoi/so giup code ro nghia va tranh nhap sai gia tri. Cac gia tri nay duoc luu
 * vao DB duoi dang chuoi ten (xem @Enumerated(EnumType.STRING) trong Payment).</p>
 *
 * <p>Tu khoa (keywords): enum, enumeration, type-safe constants.</p>
 */
public enum PaymentMethod {
    CASH,      // tien mat
    CARD,      // the
    TRANSFER   // chuyen khoan
}
