package com.coford.cafe.order.service;

import com.coford.cafe.common.exception.ApiException;
import com.coford.cafe.common.idempotency.IdempotencyService;
import com.coford.cafe.kitchen.KitchenEvent;
import com.coford.cafe.kitchen.KitchenEventPublisher;
import com.coford.cafe.menu.domain.MenuItem;
import com.coford.cafe.menu.repository.MenuItemRepository;
import com.coford.cafe.order.domain.OrderEntity;
import com.coford.cafe.order.domain.OrderItem;
import com.coford.cafe.order.domain.OrderStatus;
import com.coford.cafe.order.repository.OrderRepository;
import com.coford.cafe.order.web.dto.CreateOrderRequest;
import com.coford.cafe.order.web.dto.OrderLineRequest;
import com.coford.cafe.order.web.dto.OrderResponse;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service (tang nghiep vu) cho don hang - chua toan bo logic xu ly don.
 *
 * <p>Tang service nam giua Controller (web) va Repository (DB): controller nhan request roi goi
 * service; service ap dung quy tac nghiep vu (business rule), goi repository, va phoi hop voi cac
 * thanh phan khac (idempotency, Kafka, metrics).</p>
 *
 * <p><b>@Transactional o cap class</b>: moi method public se chay trong 1 giao dich (transaction)
 * cua DB. Neu method nem exception, transaction se ROLLBACK (huy moi thay doi) -> dam bao tinh
 * nhat quan (atomicity: hoac thanh cong tat ca, hoac khong gi ca). Cac method chi doc duoc danh
 * dau readOnly = true de toi uu (khong can flush/dirty-checking).</p>
 *
 * <p><b>Vi sao thay doi entity khong can goi save()?</b> Trong 1 transaction, entity lay tu DB la
 * "managed" (duoc quan ly). Khi commit, Hibernate tu phat hien thay doi (dirty checking) va UPDATE
 * tu dong. Vi vay cac method nhu prepare/cancel/updateItems chi sua entity la du.</p>
 *
 * Tu khoa: service layer, transaction, @Transactional, rollback, idempotency, event-driven,
 *          Kafka, Micrometer metrics, dirty checking, dependency injection
 */
@Service // Danh dau day la 1 Spring bean tang service -> Spring tu tao va tiem (inject) vao noi can
@Transactional // Moi method public chay trong 1 transaction DB (rollback neu loi)
public class OrderService {

    // Cac phu thuoc (dependencies) duoc tiem qua constructor (constructor injection - cach khuyen dung).
    private final OrderRepository orderRepo;       // truy cap bang orders
    private final MenuItemRepository menuItemRepo; // tra cuu mon trong menu (lay gia/ten/tinh trang ban)
    private final IdempotencyService idempotency;  // luu/tra ket qua theo Idempotency-Key (chong tao trung)
    private final KitchenEventPublisher kitchen;   // phat su kien (event) len Kafka cho man bep
    private final MeterRegistry meter;             // Micrometer: ghi nhan metric (so don tao...)

    // Constructor: Spring tu dong truyen cac bean phu hop vao (dependency injection).
    public OrderService(OrderRepository orderRepo, MenuItemRepository menuItemRepo,
                        IdempotencyService idempotency, KitchenEventPublisher kitchen, MeterRegistry meter) {
        this.orderRepo = orderRepo;
        this.menuItemRepo = menuItemRepo;
        this.idempotency = idempotency;
        this.kitchen = kitchen;
        this.meter = meter;
    }

    /**
     * Tao don hang moi - co bao ve TINH IDEMPOTENT (idempotency).
     *
     * <p>Idempotency: cung 1 request gui nhieu lan (vd: client bam 2 lan, hoac mang loi roi retry)
     * chi tao DUNG 1 don. Client gui header "Idempotency-Key" (1 chuoi duy nhat cho moi y dinh tao).
     * Server luu lai (key -> id don da tao). Lan sau cung key -> tra lai don cu thay vi tao moi.</p>
     */
    public OrderResponse create(CreateOrderRequest req, String idempotencyKey) {
        // Nếu request này đã xử lý (cùng Idempotency-Key) -> trả lại đơn cũ, không tạo mới.
        // findResult tra ve id don da tao truoc do (neu key da tung dung).
        Optional<Long> existing = idempotency.findResult("order", idempotencyKey);
        if (existing.isPresent()) {
            return get(existing.get()); // tra lai don cu -> idempotent, khong tao trung
        }
        // Chua tung xu ly key nay -> tao don that su.
        OrderResponse created = doCreate(req);
        // Luu anh xa key -> id don, de lan retry sau nhan ra va tra ve don nay.
        idempotency.saveResult("order", idempotencyKey, created.id());
        // Phat su kien "don moi" len Kafka -> man bep (kitchen board) nhan duoc theo kieu event-driven
        // (cac thanh phan khong goi truc tiep nhau, ma giao tiep qua su kien -> noi long ket noi).
        kitchen.publish(KitchenEvent.created(created)); // đẩy lên màn bếp qua Kafka
        // Tang bo dem (counter) metric nghiep vu: dem so don da tao. Micrometer xuat ra ten
        // "coford_orders_total" cho he thong giam sat (vd: Prometheus) theo doi.
        meter.counter("coford.orders").increment(); // metric nghiệp vụ -> coford_orders_total
        return created;
    }

    /**
     * Lay danh sach don cho MAN BEP (kitchen board): cac don CHUA pha xong va CHUA bi huy,
     * sap xep cu nhat len truoc (de bep lam theo thu tu).
     * readOnly = true: transaction chi doc -> toi uu, khong ghi DB.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> kitchenBoard() {
        // Truyen CANCELLED de loai don da huy (StatusNot). Query da @EntityGraph nap san items (tranh N+1).
        return orderRepo.findByPreparedFalseAndStatusNotOrderByCreatedAtAsc(OrderStatus.CANCELLED)
                .stream().map(OrderResponse::from).toList(); // chuyen entity -> DTO tra ve client
    }

    /**
     * Sửa danh sách món của đơn chưa thanh toán (thêm/bớt/sửa/xóa).
     *
     * <p>Quy tac nghiep vu: CHI sua duoc don dang o trang thai NEW. Don da PAID/CANCELLED khong cho sua.</p>
     * <p>Cach lam: xoa sach mon cu roi them lai theo danh sach moi (replace toan bo). orphanRemoval = true
     * tren quan he items se tu xoa cac OrderItem cu khoi DB khi clear().</p>
     */
    public OrderResponse updateItems(Long id, List<OrderLineRequest> lines) {
        OrderEntity order = findOrThrow(id);
        // Kiem tra dieu kien: chi cho sua don NEW. Neu khong -> nem loi 400 (bad request).
        if (order.getStatus() != OrderStatus.NEW) {
            throw ApiException.badRequest("Chỉ sửa được đơn chưa thanh toán");
        }
        // Xoa het mon cu. Nho orphanRemoval=true, cac OrderItem bi mo coi (mat cha) se bi xoa khoi DB.
        order.getItems().clear();
        for (OrderLineRequest line : lines) {
            // Tra cuu mon trong menu de lay snapshot ten/gia HIEN TAI; neu khong co -> loi 404.
            MenuItem menuItem = menuItemRepo.findById(line.menuItemId())
                    .orElseThrow(() -> ApiException.notFound("Khong tim thay mon id=" + line.menuItemId()));
            // Mon dang tat ban (not available) thi khong cho them vao don.
            if (!menuItem.isAvailable()) {
                throw ApiException.badRequest("Mon '" + menuItem.getName() + "' hien khong ban");
            }
            // Tao dong mon moi voi snapshot ten/gia tu menu hien tai; addItem se tinh lai tong tien.
            order.addItem(new OrderItem(menuItem.getId(), menuItem.getName(), menuItem.getPrice(),
                    line.quantity(), line.note()));
        }
        OrderResponse res = OrderResponse.from(order);
        kitchen.publish(KitchenEvent.created(res)); // cập nhật lại màn bếp (phat lai event de bep thay doi moi)
        return res;
        // Khong goi save(): order la entity managed trong transaction -> Hibernate tu UPDATE khi commit.
    }

    /**
     * Danh dau don da pha che XONG (tu man bep). Set prepared = true va phat event "prepared".
     */
    public OrderResponse prepare(Long id) {
        OrderEntity order = findOrThrow(id);
        order.setPrepared(true); // thay doi nay se duoc Hibernate tu luu khi commit (dirty checking)
        kitchen.publish(KitchenEvent.prepared(id)); // bao man bep: don nay da xong -> go khoi hang doi
        return OrderResponse.from(order);
    }

    /**
     * Logic tao don THUC SU (tach rieng khoi create() de phan idempotency goi lai duoc).
     * private: chi dung noi bo class nay.
     */
    private OrderResponse doCreate(CreateOrderRequest req) {
        OrderEntity order = new OrderEntity(req.tableNo(), req.note());
        for (OrderLineRequest line : req.items()) {
            // Voi moi dong yeu cau: tra cuu mon trong menu (404 neu khong co).
            MenuItem menuItem = menuItemRepo.findById(line.menuItemId())
                    .orElseThrow(() -> ApiException.notFound("Khong tim thay mon id=" + line.menuItemId()));
            // Khong cho dat mon dang tat ban.
            if (!menuItem.isAvailable()) {
                throw ApiException.badRequest("Mon '" + menuItem.getName() + "' hien khong ban");
            }
            // Chup snapshot ten + gia tu menu vao OrderItem (gia luc dat hang duoc "dong bang").
            order.addItem(new OrderItem(
                    menuItem.getId(),
                    menuItem.getName(),
                    menuItem.getPrice(),
                    line.quantity(),
                    line.note()));
        }
        // save(order): luu don + (cascade ALL) cac dong mon xuong DB, roi chuyen sang DTO tra ve.
        return OrderResponse.from(orderRepo.save(order));
    }

    /**
     * Liet ke don: neu co truyen status -> loc theo trang thai; neu null -> lay tat ca (moi nhat truoc).
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> list(OrderStatus status) {
        List<OrderEntity> orders = (status != null)
                ? orderRepo.findByStatus(status)               // co loc trang thai (da @EntityGraph -> tranh N+1)
                : orderRepo.findAllByOrderByCreatedAtDesc();   // khong loc -> tat ca, moi nhat len truoc
        return orders.stream().map(OrderResponse::from).toList();
    }

    /** Lay chi tiet 1 don theo id (404 neu khong co). */
    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return OrderResponse.from(findOrThrow(id));
    }

    /**
     * Huy don. Quy tac: don da PAID thi khong huy duoc. Con lai -> chuyen trang thai sang CANCELLED.
     */
    public OrderResponse cancel(Long id) {
        OrderEntity order = findOrThrow(id);
        if (order.getStatus() == OrderStatus.PAID) {
            throw ApiException.badRequest("Don da thanh toan, khong the huy");
        }
        order.setStatus(OrderStatus.CANCELLED); // Hibernate tu UPDATE khi commit
        return OrderResponse.from(order);
    }

    /**
     * Tien ich noi bo: tim don theo id, neu khong thay thi nem loi 404 (not found).
     * package-private de cac lop test/cung package co the dung.
     */
    OrderEntity findOrThrow(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay don id=" + id));
    }
}
