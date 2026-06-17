package com.coford.cafe.menu.service;

import com.coford.cafe.common.config.CacheConfig;
import com.coford.cafe.common.exception.ApiException;
import com.coford.cafe.menu.domain.Category;
import com.coford.cafe.menu.domain.MenuItem;
import com.coford.cafe.menu.repository.CategoryRepository;
import com.coford.cafe.menu.repository.MenuItemRepository;
import com.coford.cafe.menu.web.dto.CategoryRequest;
import com.coford.cafe.menu.web.dto.CategoryResponse;
import com.coford.cafe.menu.web.dto.MenuItemRequest;
import com.coford.cafe.menu.web.dto.MenuItemResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MenuService - Lớp DỊCH VỤ (service) chứa logic nghiệp vụ (business logic) cho menu:
 * quản lý danh mục ({@link Category}) và món ({@link MenuItem}).
 *
 * <p>Khái niệm CACHE-ASIDE (cache đặt bên cạnh DB):
 * <ul>
 *   <li>Khi ĐỌC: ứng dụng tìm trong cache trước. Nếu có (cache hit) -> trả về luôn, không vào DB.
 *       Nếu chưa có (cache miss) -> truy vấn DB, rồi LƯU kết quả vào cache cho lần sau.
 *       Đây là vai trò của {@code @Cacheable}.</li>
 *   <li>Khi GHI (thêm/sửa/xóa): dữ liệu trong cache trở nên cũ (stale) -> phải XÓA cache
 *       để lần đọc sau nạp lại dữ liệu mới từ DB. Đây là vai trò của {@code @CacheEvict}.</li>
 * </ul>
 * Spring Cache abstraction điều phối việc này qua annotation; ở dự án này cache store
 * (kho lưu cache) thực tế là Caffeine (cache in-memory tốc độ cao).</p>
 *
 * <p>Từ khóa: Spring Cache abstraction, cache-aside, Caffeine, @Cacheable, @CacheEvict,
 * service layer, business logic, @Transactional, DTO mapping.</p>
 */
// @Service: đánh dấu đây là một Spring bean ở tầng service -> Spring tự quản lý vòng đời và tiêm (inject).
@Service
// @Transactional ở cấp class: MỌI method public chạy trong một GIAO DỊCH (transaction) DB.
// Nếu có lỗi (exception) -> toàn bộ thay đổi được hoàn tác (rollback) để giữ dữ liệu nhất quán.
@Transactional
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);

    // Hai repository được tiêm vào (dependency injection) qua constructor bên dưới.
    private final CategoryRepository categoryRepo;
    private final MenuItemRepository menuItemRepo;

    // Constructor injection: Spring tự truyền (inject) các bean repository vào khi tạo MenuService.
    // Đây là cách tiêm phụ thuộc được khuyến nghị (constructor injection) -> field final, dễ test.
    public MenuService(CategoryRepository categoryRepo, MenuItemRepository menuItemRepo) {
        this.categoryRepo = categoryRepo;
        this.menuItemRepo = menuItemRepo;
    }

    /**
     * Lấy danh sách tất cả danh mục (đã sắp xếp theo sortOrder).
     * readOnly = true: tối ưu cho giao dịch CHỈ ĐỌC (không ghi) -> Hibernate bỏ qua dirty checking.
     * @Cacheable: lần đầu gọi sẽ vào DB rồi lưu kết quả vào cache "CATEGORIES"; các lần sau
     *             lấy thẳng từ cache (không in log "[DB]" nữa vì method không thực sự chạy lại).
     */
    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.CATEGORIES)
    public List<CategoryResponse> listCategories() {
        // Log này CHỈ xuất hiện khi cache miss (cache chưa có dữ liệu) -> dấu hiệu thực sự truy vấn DB.
        log.info("[DB] tải danh mục từ database (cache miss)");
        // Lấy entity từ DB -> dùng stream() chuyển mỗi Category thành CategoryResponse (DTO) qua method
        // tham chiếu (method reference) CategoryResponse::from -> gom lại thành List bất biến (toList).
        return categoryRepo.findAllByOrderBySortOrderAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * Tạo mới một danh mục.
     * @CacheEvict(allEntries = true): vì vừa GHI dữ liệu mới, XÓA TOÀN BỘ (allEntries) cache
     * "CATEGORIES" để lần đọc kế tiếp nạp lại danh sách mới nhất từ DB (tránh dữ liệu cũ - stale).
     */
    @CacheEvict(cacheNames = CacheConfig.CATEGORIES, allEntries = true)
    public CategoryResponse createCategory(CategoryRequest req) {
        // Tạo entity Category từ dữ liệu request rồi save() xuống DB; save trả về entity đã có id.
        Category saved = categoryRepo.save(new Category(req.name(), req.sortOrder()));
        // Chuyển entity đã lưu thành DTO trả về cho client (không lộ entity ra ngoài tầng web).
        return CategoryResponse.from(saved);
    }

    /**
     * Lấy danh sách món, có thể LỌC theo danh mục hoặc chỉ lấy món đang bán.
     * @Cacheable("MENU_ITEMS"): cache theo tham số (categoryId, onlyAvailable) -> mỗi tổ hợp tham số
     * là một khóa cache (cache key) riêng. Lần đầu vào DB, các lần sau lấy từ cache.
     */
    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.MENU_ITEMS)
    public List<MenuItemResponse> listItems(Long categoryId, boolean onlyAvailable) {
        log.info("[DB] tải món từ database (cache miss) categoryId={} onlyAvailable={}", categoryId, onlyAvailable);
        List<MenuItem> items;
        // Ưu tiên lọc theo danh mục nếu có categoryId; nếu không thì xét onlyAvailable; cuối cùng lấy tất cả.
        if (categoryId != null) {
            items = menuItemRepo.findByCategoryId(categoryId);
        } else if (onlyAvailable) {
            items = menuItemRepo.findByAvailableTrue();
        } else {
            items = menuItemRepo.findAll();
        }
        return items.stream().map(MenuItemResponse::from).toList();
    }

    /**
     * Thêm món mới. @CacheEvict xóa toàn bộ cache "MENU_ITEMS" vì danh sách món đã thay đổi.
     */
    @CacheEvict(cacheNames = CacheConfig.MENU_ITEMS, allEntries = true)
    public MenuItemResponse createItem(MenuItemRequest req) {
        // Tìm danh mục theo id; nếu không thấy -> ném lỗi 404 (notFound) thay vì để NullPointerException.
        // findById trả về Optional; orElseThrow ném exception khi Optional rỗng.
        Category category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> ApiException.notFound("Khong tim thay danh muc id=" + req.categoryId()));
        MenuItem item = new MenuItem(
                category,
                req.name(),
                req.description(),
                req.price(),
                // available có thể null trong request -> coi như mặc định true (đang bán)
                // bằng biểu thức: null HOẶC giá trị true.
                req.available() == null || req.available());
        return MenuItemResponse.from(menuItemRepo.save(item));
    }

    /**
     * Cập nhật một món đang có. Xóa cache "MENU_ITEMS" sau khi sửa.
     * Lưu ý: trong transaction, chỉ cần sửa các thuộc tính của entity đã managed (được JPA theo dõi);
     * Hibernate tự động ghi thay đổi xuống DB khi commit (dirty checking) -> không cần gọi save() lại.
     */
    @CacheEvict(cacheNames = CacheConfig.MENU_ITEMS, allEntries = true)
    public MenuItemResponse updateItem(Long id, MenuItemRequest req) {
        // Tìm món cần sửa; không thấy -> 404.
        MenuItem item = menuItemRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay mon id=" + id));
        // Tìm danh mục mới (món có thể được đổi sang danh mục khác); không thấy -> 404.
        Category category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> ApiException.notFound("Khong tim thay danh muc id=" + req.categoryId()));
        item.setCategory(category);
        item.setName(req.name());
        item.setDescription(req.description());
        item.setPrice(req.price());
        // Chỉ cập nhật trạng thái bán khi request có gửi giá trị available (khác null).
        if (req.available() != null) {
            item.setAvailable(req.available());
        }
        return MenuItemResponse.from(item);
    }

    /**
     * Xóa một món theo id. Xóa cache "MENU_ITEMS" sau khi xóa.
     */
    @CacheEvict(cacheNames = CacheConfig.MENU_ITEMS, allEntries = true)
    public void deleteItem(Long id) {
        // Kiểm tra tồn tại trước khi xóa để trả lỗi 404 rõ ràng thay vì xóa âm thầm một id không có thật.
        if (!menuItemRepo.existsById(id)) {
            throw ApiException.notFound("Khong tim thay mon id=" + id);
        }
        menuItemRepo.deleteById(id);
    }
}
