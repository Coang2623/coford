package com.coford.cafe.menu.web.dto;

import com.coford.cafe.menu.domain.MenuItem;
import java.math.BigDecimal;

public record MenuItemResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        BigDecimal price,
        boolean available) {

    public static MenuItemResponse from(MenuItem m) {
        return new MenuItemResponse(
                m.getId(),
                m.getCategory().getId(),
                m.getCategory().getName(),
                m.getName(),
                m.getDescription(),
                m.getPrice(),
                m.isAvailable());
    }
}
