package com.coford.cafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lop khoi dong (entry point) cua toan bo ung dung Spring Boot "coford".
 *
 * <p>Vai tro trong kien truc: day la diem bat dau chuong trinh. Khi chay,
 * Spring Boot se khoi tao "application context" (kho chua tat ca cac bean/doi tuong
 * duoc Spring quan ly), tu dong cau hinh va khoi dong web server nhung (embedded
 * Tomcat) de phuc vu cac API.
 *
 * <p>Khai niem minh hoa: Auto-configuration (tu dong cau hinh), Component scanning
 * (tu dong quet va dang ky cac @Component/@Service/@Controller... trong cac package con),
 * Inversion of Control / Dependency Injection (Spring tu tao va "tiem" cac phu thuoc).
 *
 * <p>Tu khoa: Spring Boot, @SpringBootApplication, application entry point,
 * ApplicationContext, auto-configuration, component scan, embedded server.
 */
// @SpringBootApplication: annotation tong hop, tuong duong gop 3 annotation:
//   - @Configuration: cho phep lop nay khai bao cac @Bean.
//   - @EnableAutoConfiguration: bat co che tu dong cau hinh dua tren cac thu vien co trong classpath.
//   - @ComponentScan: tu dong quet cac bean trong package "com.coford.cafe" va cac package con.
@SpringBootApplication
public class CofordApplication {
    // main(): phuong thuc tieu chuan cua Java de chay chuong trinh (Java application entry point).
    public static void main(String[] args) {
        // SpringApplication.run(...): khoi tao application context va khoi dong server.
        // Tham so dau la lop cau hinh goc, "args" la cac doi so dong lenh (command-line arguments).
        SpringApplication.run(CofordApplication.class, args);
    }
}
