package com.coford.cafe.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Phase 3 — Local cache (L1) bằng Caffeine cho dữ liệu menu (đọc nhiều, đổi ít).
 * Caffeine giữ cache trong RAM tiến trình, rất nhanh. Redis dùng riêng cho idempotency
 * (xem IdempotencyService) để chia sẻ giữa nhiều instance.
 *
 * <p>Vai tro trong kien truc: lop cau hinh khai bao mot bo nho dem (cache) cuc bo nam ngay
 * trong RAM cua tien trinh ung dung. Du lieu menu (danh muc, mon) it thay doi nhung doc rat
 * nhieu, nen luu cache giup giam so lan truy van database va tang toc do phan hoi.
 *
 * <p>Khai niem minh hoa:
 * <ul>
 *   <li>Cache nhieu tang (multi-level cache): L1 = cache cuc bo (Caffeine, trong RAM tien trinh),
 *       trong khi Redis la cache phan tan dung cho viec khac (idempotency).</li>
 *   <li>Spring Cache Abstraction: cho phep dung cac annotation nhu @Cacheable / @CacheEvict
 *       o tang service ma khong phai tu viet logic luu/xoa cache.</li>
 *   <li>Cache eviction policy: chinh sach loai bo phan tu khi cache day hoac het han.</li>
 * </ul>
 *
 * <p>Tu khoa: Caffeine cache, Spring Cache Abstraction, @EnableCaching, @Cacheable,
 * in-memory cache, L1 cache, cache eviction, TTL (time-to-live).
 */
// @Configuration: lop cau hinh chua dinh nghia bean.
// @EnableCaching: BAT co che cache cua Spring. Sau khi bat, cac annotation nhu @Cacheable,
//   @CachePut, @CacheEvict moi co tac dung (Spring tao proxy chan cac loi goi method de quan ly cache).
@Configuration
@EnableCaching
public class CacheConfig {

    // Ten cac vung cache (cache name). Khai bao hang so de dung lai o cac noi @Cacheable("categories")...,
    // tranh viet chuoi "magic string" rai rac de sai chinh ta.
    public static final String CATEGORIES = "categories";
    public static final String MENU_ITEMS = "menuItems";

    // @Bean: dang ky CacheManager - doi tuong trung tam quan ly cac vung cache cho toan ung dung.
    // @Primary: neu trong context co nhieu CacheManager, day la cai duoc uu tien chon (default).
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        // Tao CacheManager dua tren Caffeine, khai bao truoc 2 vung cache: "categories" va "menuItems".
        CaffeineCacheManager manager = new CaffeineCacheManager(CATEGORIES, MENU_ITEMS);
        // setCaffeine(...): ap dung cau hinh chung cho cac cache do CacheManager quan ly.
        // Builder pattern: noi tiep cac tuy chon cau hinh.
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)                          // Gioi han toi da 500 phan tu; vuot qua se bi loai bot (eviction).
                .expireAfterWrite(Duration.ofMinutes(10))  // Het han sau 10 phut ke tu luc GHI (write); doc lai se nap moi tu DB.
                .recordStats());                           // Ghi lai thong ke (hit/miss) de theo doi hieu qua cache.
        return manager;
    }
}
