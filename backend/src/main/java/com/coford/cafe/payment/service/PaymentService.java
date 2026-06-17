package com.coford.cafe.payment.service;

import com.coford.cafe.common.exception.ApiException;
import com.coford.cafe.common.idempotency.IdempotencyService;
import com.coford.cafe.order.domain.OrderEntity;
import com.coford.cafe.order.domain.OrderStatus;
import com.coford.cafe.order.repository.OrderRepository;
import com.coford.cafe.payment.domain.Payment;
import com.coford.cafe.payment.repository.PaymentRepository;
import com.coford.cafe.payment.web.dto.InvoiceLine;
import com.coford.cafe.payment.web.dto.InvoiceResponse;
import com.coford.cafe.payment.web.dto.PayRequest;
import com.coford.cafe.payment.web.dto.PaymentResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tang dich vu (service layer) xu ly nghiep vu thanh toan.
 *
 * <p>Service la noi chua logic nghiep vu (business logic): kiem tra dieu kien,
 * dieu phoi giua cac repository, va dam bao tinh nhat quan du lieu.</p>
 *
 * <p>{@code @Transactional} o cap class: moi phuong thuc public se chay trong MOT
 * giao dich (transaction) DB. Neu giua chung co loi/exception, moi thay doi se bi
 * thu hoi (rollback) -> du lieu khong roi vao trang thai do dang. Day la tinh
 * "all-or-nothing" cua transaction.</p>
 *
 * <p>Luong pay() bao gom: kiem tra trang thai don (state machine), chong thanh toan
 * trung (idempotency + existsByOrderId), tao ban ghi Payment, chuyen don sang PAID,
 * va ghi metric (so dem theo phuong thuc). Luong invoice() dung de dung hoa don.</p>
 *
 * <p>Tu khoa (keywords): service layer, transaction, @Transactional, idempotency,
 * state machine, business logic.</p>
 */
@Service // Danh dau day la Spring bean thuoc tang service -> Spring tu quan ly va tiem (inject).
@Transactional // Moi phuong thuc chay trong 1 transaction; loi se rollback toan bo.
public class PaymentService {

    // Cac phu thuoc (dependencies) duoc Spring tiem vao qua constructor (constructor injection).
    // Dat 'final' de bao dam khong bi gan lai sau khi khoi tao.
    private final PaymentRepository paymentRepo; // truy van/luu ban ghi thanh toan
    private final OrderRepository orderRepo;     // truy van/cap nhat don hang
    private final IdempotencyService idempotency; // luu/tra ket qua theo Idempotency-Key
    private final MeterRegistry meter;           // ghi metric (Micrometer) de theo doi/giam sat

    // Constructor injection: Spring tu dong truyen cac bean tuong ung vao day khi tao PaymentService.
    public PaymentService(PaymentRepository paymentRepo, OrderRepository orderRepo,
                          IdempotencyService idempotency, MeterRegistry meter) {
        this.paymentRepo = paymentRepo;
        this.orderRepo = orderRepo;
        this.idempotency = idempotency;
        this.meter = meter;
    }

    /**
     * Thanh toan mot don hang.
     *
     * <p>Cac buoc xu ly (theo thu tu):</p>
     * <ol>
     *   <li><b>Chong xu ly trung theo Idempotency-Key:</b> neu key da tung duoc xu ly
     *       (client goi lai do mang chap chon/bam nut 2 lan), ta KHONG thanh toan lai
     *       ma tra lai ket qua cu. Day la tinh "idempotent": goi nhieu lan cung cho
     *       cung mot ket qua, khong thu tien lan thu hai.</li>
     *   <li><b>Kiem tra trang thai don (state machine):</b> khong cho thanh toan don da
     *       bi huy (CANCELLED) hoac da thanh toan (PAID). Trang thai don chi di theo
     *       cac buoc hop le -> day la tu duy "may trang thai" (state machine).</li>
     *   <li><b>Chong thanh toan trung o tang DB:</b> existsByOrderId kiem tra da co
     *       ban thanh toan chua (ket hop voi rang buoc unique tren cot order_id).</li>
     *   <li><b>Tao ban ghi Payment</b> voi so tien lay tu tong tien don hang.</li>
     *   <li><b>Chuyen trang thai don sang PAID.</b></li>
     *   <li><b>Luu ket qua theo Idempotency-Key</b> de lan goi sau (cung key) tra ngay ket qua cu.</li>
     *   <li><b>Ghi metric:</b> tang bo dem (counter) so luot thanh toan theo tung phuong thuc.</li>
     * </ol>
     *
     * @param orderId        id don hang can thanh toan
     * @param req            yeu cau thanh toan (chua phuong thuc)
     * @param idempotencyKey khoa chong trung do client gui qua header Idempotency-Key (co the null)
     */
    public PaymentResponse pay(Long orderId, PayRequest req, String idempotencyKey) {
        // (1) Neu cung Idempotency-Key da duoc xu ly truoc do -> tra lai ket qua thanh toan cu,
        //     khong thu tien them lan nua (dam bao tinh idempotent khi client retry).
        if (idempotency.findResult("pay", idempotencyKey).isPresent()) {
            return paymentRepo.findByOrderId(orderId)
                    .map(PaymentResponse::from) // chuyen entity -> DTO tra ve
                    .orElseThrow(() -> ApiException.notFound("Khong tim thay thanh toan cho don id=" + orderId));
        }
        // Tim don hang; neu khong co thi nem loi 404 (not found).
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay don id=" + orderId));
        // (2) State machine: don da huy thi khong duoc thanh toan.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw ApiException.badRequest("Don da bi huy, khong the thanh toan");
        }
        // (2)+(3) Don da PAID hoac da ton tai ban thanh toan -> chong thanh toan trung.
        if (order.getStatus() == OrderStatus.PAID || paymentRepo.existsByOrderId(orderId)) {
            throw ApiException.badRequest("Don da duoc thanh toan");
        }
        // (4) Tao va luu ban ghi thanh toan; so tien lay theo tong tien cua don hang.
        Payment payment = paymentRepo.save(new Payment(orderId, req.method(), order.getTotalAmount()));
        // (5) Chuyen trang thai don sang PAID (vi dang trong @Transactional, thay doi nay
        //     se duoc Hibernate tu dong luu xuong DB khi ket thuc giao dich - co che "dirty checking").
        order.setStatus(OrderStatus.PAID);
        // (6) Ghi nho ket qua theo Idempotency-Key cho lan goi lai sau nay.
        idempotency.saveResult("pay", idempotencyKey, orderId);
        // (7) Tang counter metric, gan nhan (tag) "method" theo phuong thuc thanh toan,
        //     phuc vu giam sat/thong ke (Micrometer). Tu khoa: meter counter, metrics.
        meter.counter("coford.payments", "method", req.method().name()).increment(); // metric theo phương thức
        // Tra ve DTO ket qua thanh toan cho tang web.
        return PaymentResponse.from(payment);
    }

    /**
     * Dung va tra ve hoa don (invoice) cho mot don da thanh toan.
     *
     * <p>{@code @Transactional(readOnly = true)}: giao dich chi doc, khong ghi.
     * Bao cho DB/Hibernate biet de toi uu (vi du bo qua dirty checking).</p>
     *
     * <p>Logic: tim ban thanh toan (neu chua co -> bao don chua thanh toan), tim don hang,
     * roi chuyen tung dong san pham (order item) thanh dong hoa don (InvoiceLine) bang Stream API.</p>
     */
    @Transactional(readOnly = true)
    public InvoiceResponse invoice(Long orderId) {
        // Neu chua co ban thanh toan -> coi nhu don chua thanh toan, nem loi 404.
        Payment payment = paymentRepo.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.notFound("Don chua duoc thanh toan"));
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay don id=" + orderId));
        // Duyet tung mon trong don (Stream API): map moi mon thanh 1 dong hoa don InvoiceLine,
        // roi gom lai thanh danh sach (List) bat bien.
        var lines = order.getItems().stream()
                .map(i -> new InvoiceLine(i.getItemName(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
                .toList();
        // Lap rap doi tuong hoa don tra ve cho tang web.
        return new InvoiceResponse(
                order.getId(), order.getTableNo(), lines,
                order.getTotalAmount(), payment.getMethod(), payment.getPaidAt());
    }
}
