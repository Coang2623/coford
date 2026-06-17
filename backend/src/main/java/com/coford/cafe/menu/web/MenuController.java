package com.coford.cafe.menu.web;

import com.coford.cafe.menu.service.MenuService;
import com.coford.cafe.menu.web.dto.CategoryRequest;
import com.coford.cafe.menu.web.dto.CategoryResponse;
import com.coford.cafe.menu.web.dto.MenuItemRequest;
import com.coford.cafe.menu.web.dto.MenuItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * MenuController - Lớp ĐIỀU KHIỂN (controller) REST cho phần menu: nhận HTTP request từ client,
 * gọi {@link MenuService} xử lý, rồi trả về kết quả dạng JSON.
 *
 * <p>Khái niệm: đây là một Spring MVC REST controller. Mỗi method ánh xạ tới một endpoint
 * (đường dẫn + phương thức HTTP). Controller chỉ điều phối (mỏng), logic nghiệp vụ nằm ở service.</p>
 *
 * <p>Từ khóa: Spring MVC REST controller, REST API, HTTP endpoint, JSON serialization,
 * OpenAPI/Swagger documentation.</p>
 */
// @RestController = @Controller + @ResponseBody: mọi giá trị trả về được tự động chuyển thành
// JSON (qua Jackson) và ghi vào body của HTTP response.
@RestController
// @RequestMapping: tiền tố (base path) chung cho mọi endpoint trong controller này: "/api/menu".
@RequestMapping("/api/menu")
// @Tag (OpenAPI/Swagger): gom nhóm các API này dưới nhãn "Menu" trong tài liệu Swagger UI tự sinh.
@Tag(name = "Menu", description = "Quan ly danh muc va mon")
public class MenuController {

    private final MenuService service;

    // Constructor injection: Spring tự tiêm MenuService vào controller.
    public MenuController(MenuService service) {
        this.service = service;
    }

    // @GetMapping: xử lý HTTP GET tại "/api/menu/categories" -> đọc danh sách danh mục.
    // @Operation (OpenAPI): mô tả ngắn gọn endpoint hiển thị trong tài liệu Swagger.
    @GetMapping("/categories")
    @Operation(summary = "Lấy danh sách danh mục")
    public List<CategoryResponse> listCategories() {
        return service.listCategories();
    }

    // @PostMapping: xử lý HTTP POST -> TẠO mới danh mục.
    // @ResponseStatus(CREATED): trả mã HTTP 201 (Created) khi thành công thay vì 200 mặc định.
    // @Valid: kích hoạt KIỂM TRA dữ liệu (Bean Validation) trên body request trước khi vào method.
    // @RequestBody: ánh xạ JSON trong body request thành đối tượng CategoryRequest.
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo danh mục")
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest req) {
        return service.createCategory(req);
    }

    // GET "/api/menu/items" với các tham số truy vấn (query parameter) tùy chọn.
    // @RequestParam(required = false): tham số ?categoryId=... là KHÔNG bắt buộc (có thể null).
    // @RequestParam(defaultValue = "false"): nếu thiếu ?onlyAvailable thì mặc định = false.
    @GetMapping("/items")
    @Operation(summary = "Danh sách món (loc theo danh mục hoặc chỉ món đang bán)")
    public List<MenuItemResponse> listItems(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") boolean onlyAvailable) {
        return service.listItems(categoryId, onlyAvailable);
    }

    // POST tạo món mới; trả 201 Created; validate body MenuItemRequest qua @Valid.
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm món mới")
    public MenuItemResponse createItem(@Valid @RequestBody MenuItemRequest req) {
        return service.createItem(req);
    }

    // @PutMapping("/items/{id}"): xử lý HTTP PUT -> CẬP NHẬT món có id chỉ định.
    // @PathVariable Long id: lấy giá trị {id} từ ĐƯỜNG DẪN URL (path) gán vào tham số id.
    @PutMapping("/items/{id}")
    @Operation(summary = "Cập nhật món")
    public MenuItemResponse updateItem(@PathVariable Long id, @Valid @RequestBody MenuItemRequest req) {
        return service.updateItem(id, req);
    }

    // @DeleteMapping: xử lý HTTP DELETE -> XÓA món.
    // @ResponseStatus(NO_CONTENT): trả mã 204 (No Content) - xóa thành công, không có body trả về.
    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa món")
    public void deleteItem(@PathVariable Long id) {
        service.deleteItem(id);
    }
}
