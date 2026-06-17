package com.coford.cafe.payment.repository;

import com.coford.cafe.payment.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository (kho du lieu) cho thuc the {@link Payment}.
 *
 * <p>Ke thua {@link JpaRepository} nen tu dong co san cac thao tac CRUD co ban
 * (save, findById, findAll, delete...). Spring Data JPA se TU DONG sinh phan
 * trien khai (implementation) cho interface nay luc chay, ta khong can tu viet.</p>
 *
 * <p>Hai phuong thuc duoi dung co che "derived query method": Spring doc TEN
 * phuong thuc (findByOrderId, existsByOrderId) va tu sinh cau lenh truy van
 * tuong ung theo truong orderId.</p>
 *
 * <p>Tu khoa (keywords): Spring Data JPA, JpaRepository, derived query method, CRUD.</p>
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Tim ban thanh toan theo orderId. Tra ve Optional (co the rong neu chua thanh toan),
    // giup tranh loi NullPointerException khi khong tim thay.
    Optional<Payment> findByOrderId(Long orderId);

    // Kiem tra da ton tai ban thanh toan cho orderId hay chua (tra ve true/false).
    // Dung de chong thanh toan trung (xem PaymentService.pay()).
    boolean existsByOrderId(Long orderId);
}
