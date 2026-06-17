package com.coford.cafe.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cau hinh tai lieu API theo chuan OpenAPI 3 (xem tai /swagger-ui.html).
 *
 * <p>Vai tro trong kien truc: lop cau hinh (configuration class) cung cap thong tin
 * mo ta API (tieu de, mo ta, phien ban). Thu vien springdoc-openapi se dua thong tin
 * nay vao trang tai lieu tu dong sinh ra, giup lap trinh vien xem va thu goi API
 * ngay tren trinh duyet (Swagger UI).
 *
 * <p>Khai niem minh hoa: dinh nghia mot @Bean trong mot @Configuration de tuy bien
 * hanh vi mac dinh cua Spring/springdoc.
 *
 * <p>Tu khoa: OpenAPI 3, Swagger UI, springdoc-openapi, API documentation,
 * @Configuration, @Bean.
 */
// @Configuration: danh dau day la lop cau hinh, noi co the khai bao cac @Bean cho Spring quan ly.
@Configuration
public class OpenApiConfig {

    // @Bean: dang ky doi tuong tra ve (OpenAPI) vao application context.
    // springdoc se tu dong tim bean OpenAPI nay de xay dung tai lieu API.
    @Bean
    public OpenAPI cofordOpenAPI() {
        // Builder pattern: goi noi tiep cac phuong thuc de cau hinh tung phan thong tin (metadata) cua API.
        return new OpenAPI().info(new Info()
                .title("Coford - Order Ca phe API")        // Tieu de hien thi tren trang tai lieu.
                .description("API cho ung dung order ca phe noi bo") // Mo ta ngan ve API.
                .version("v1"));                            // Phien ban API (API version).
    }
}
