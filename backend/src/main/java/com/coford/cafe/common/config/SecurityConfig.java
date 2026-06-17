package com.coford.cafe.common.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Phase 5 — Bảo vệ API bằng JWT của Keycloak.
 * Vai trò: STAFF (order/thu ngân/bếp), MANAGER (thêm: menu ghi, báo cáo, ngân hàng).
 *
 * <p>Vai tro trong kien truc: day la lop cau hinh bao mat trung tam cua ung dung.
 * Ung dung dong vai tro <b>OAuth2 Resource Server</b>: no KHONG tu xu ly dang nhap, ma chi
 * <b>kiem tra</b> token JWT do mot may chu uy quyen (Authorization Server) la <b>Keycloak</b> cap.
 * Moi request goi vao API phai mang theo header "Authorization: Bearer &lt;token&gt;".
 * Spring Security se: (1) xac thuc chu ky/han su dung cua token dua tren issuer-uri (cau hinh trong
 * application.yml/properties), (2) doc cac "claim" (thong tin) trong token de biet nguoi dung co
 * vai tro gi, roi (3) ap dung phan quyen (authorization) theo tung duong dan.
 *
 * <p>Cac khai niem chinh:
 * <ul>
 *   <li><b>OAuth2 Resource Server</b>: ung dung bao ve tai nguyen (API) va tin cay token tu ben thu ba.</li>
 *   <li><b>JWT (JSON Web Token)</b>: token dang chuoi co chu ky so, chua cac claim (vd: user, vai tro).
 *       Resource server xac minh chu ky bang khoa cong khai lay tu Keycloak (qua issuer-uri / JWKS).</li>
 *   <li><b>issuer-uri</b>: dia chi cua Keycloak realm; Spring dung no de tu dong tai metadata
 *       (gom URL cua bo khoa cong khai) phuc vu kiem tra token. (Cau hinh ngoai file nay.)</li>
 *   <li><b>RBAC (Role-Based Access Control)</b>: phan quyen dua tren vai tro (STAFF / MANAGER).</li>
 *   <li><b>Stateless session</b>: server KHONG luu phien dang nhap; danh tinh nam hoan toan trong token,
 *       gui kem moi request. Giup de mo rong nhieu instance (scale ngang).</li>
 * </ul>
 *
 * <p>Tu khoa: Spring Security, OAuth2 Resource Server, JWT, Keycloak realm roles, RBAC,
 * SecurityFilterChain, JwtAuthenticationConverter, stateless session, issuer-uri, JWKS.
 */
// @Configuration: lop cau hinh, noi khai bao cac @Bean lien quan den bao mat.
@Configuration
public class SecurityConfig {

    // @Bean SecurityFilterChain: dinh nghia "chuoi bo loc bao mat" (security filter chain) cua Spring Security.
    //   Day la cach cau hinh bao mat theo phong cach moi (Spring Security 6, khong con extends WebSecurityConfigurerAdapter).
    //   Doi tuong HttpSecurity duoc Spring "tiem" vao (dependency injection) de ta mo ta cac quy tac.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // csrf().disable(): TAT bao ve CSRF. CSRF chu yeu danh cho ung dung dung cookie/phien;
                //   o day API stateless dung token Bearer nen khong can CSRF token.
                .csrf(csrf -> csrf.disable())
                // sessionManagement STATELESS: KHONG tao/khong dung HttpSession. Moi request tu xac thuc
                //   lai bang token. Day chinh la "stateless session".
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // authorizeHttpRequests: bat dau khai bao cac quy tac phan quyen theo URL.
                //   LUU Y: cac quy tac duoc xet THEO THU TU TU TREN XUONG; quy tac khop dau tien se duoc ap dung,
                //   nen cac rule cu the (specific) phai dat truoc rule tong quat (vd "/api/**").
                .authorizeHttpRequests(auth -> auth
                        // Công khai: tài liệu API + luồng SSE (EventSource không gửi được Bearer header)
                        // permitAll(): cho phep TAT CA truy cap, khong can dang nhap.
                        // - Swagger/OpenAPI: de doc tai lieu API ma khong can token.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // - /actuator: cac endpoint giam sat suc khoe (health), metrics... thuong de mo cho he thong
                        //   ha tang (load balancer, Prometheus) goi kiem tra; khong nen chan bang token.
                        .requestMatchers("/actuator/**").permitAll()
                        // - SSE (Server-Sent Events) cho man hinh bep: trinh duyet dung EventSource KHONG the gan
                        //   header "Authorization: Bearer", nen luong nay phai de mo (permitAll) thay vi yeu cau token.
                        .requestMatchers("/api/kitchen/stream").permitAll()
                        // Chỉ MANAGER
                        // hasRole("MANAGER"): yeu cau nguoi dung co vai tro MANAGER. (Spring tu them tien to "ROLE_"
                        //   khi so khop, nen quyen thuc su can co la "ROLE_MANAGER" — khop voi cach map o extractRoles ben duoi.)
                        .requestMatchers("/api/reports/**", "/api/bank/**").hasRole("MANAGER")
                        // Phan quyen theo CA phuong thuc HTTP: chi MANAGER moi duoc GHI menu (them/sua/xoa).
                        //   Con doc menu (GET) thi roi vao rule "/api/**".authenticated() ben duoi -> STAFF cung xem duoc.
                        .requestMatchers(HttpMethod.POST, "/api/menu/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/menu/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/menu/**").hasRole("MANAGER")
                        // Còn lại trong /api/**: cần đăng nhập (STAFF hoặc MANAGER)
                        // authenticated(): chi can co token hop le (da xac thuc), khong phan biet vai tro.
                        .requestMatchers("/api/**").authenticated()
                        // anyRequest().permitAll(): moi duong dan khac (ngoai /api/**) deu mo. Vi day la API thuan,
                        //   cac request khong thuoc /api/** chu yeu la tai nguyen tinh/cong khai.
                        .anyRequest().permitAll())
                // oauth2ResourceServer + jwt: kich hoat che do RESOURCE SERVER xac thuc bang JWT.
                //   jwtAuthenticationConverter(jwtConverter()): chi cho Spring biet cach chuyen claim trong token
                //   thanh danh sach quyen (authorities) — xem jwtConverter() ben duoi.
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())));
        // http.build(): hoan tat va tao ra doi tuong SecurityFilterChain.
        return http.build();
    }

    /** Lấy realm_access.roles trong token Keycloak -> quyền ROLE_*. */
    // JwtAuthenticationConverter: bo chuyen doi tu token JWT (sau khi da xac minh) thanh doi tuong
    //   Authentication co cac quyen. Mac dinh Spring doc claim "scope"/"scp"; o day ta tuy bien
    //   de doc vai tro tu cau truc rieng cua Keycloak.
    private JwtAuthenticationConverter jwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // setJwtGrantedAuthoritiesConverter: chi dinh ham trich xuat quyen tu token.
        //   "SecordConfig::extractRoles" la method reference tro toi ham tinh extractRoles ben duoi.
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::extractRoles);
        return converter;
    }

    // @SuppressWarnings("unchecked"): tam tat canh bao ep kieu (cast) khong an toan, vi claim trong JWT
    //   tra ve kieu Object/Map chung chung nen phai ep ve List<String> thu cong.
    @SuppressWarnings("unchecked")
    private static Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        // Keycloak dat cac vai tro cap realm trong claim "realm_access", co dang: { "roles": ["STAFF", "MANAGER"] }
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        // Neu token khong co claim nay (vd token thieu vai tro) -> tra ve danh sach quyen rong (khong co quyen gi).
        if (realmAccess == null) return List.of();
        // Lay danh sach ten vai tro; neu khong co khoa "roles" thi mac dinh la rong.
        List<String> roles = (List<String>) realmAccess.getOrDefault("roles", List.of());
        // Anh xa moi ten vai tro Keycloak (vd "MANAGER") thanh quyen Spring "ROLE_MANAGER".
        //   Day chinh la cau noi giua vai tro Keycloak va hasRole(...) o tren (hasRole tu them tien to "ROLE_").
        //   Dung Stream API: map tung phan tu roi gom lai thanh List.
        return roles.stream().map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r)).toList();
    }
}
