package com.coford.cafe.order.repository;

import com.coford.cafe.order.domain.OrderEntity;
import com.coford.cafe.order.domain.OrderStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository (kho du lieu) cho OrderEntity - lop truy cap DB.
 *
 * <p>Chi can KHAI BAO interface ke thua {@link JpaRepository}, Spring Data JPA se TU DONG
 * sinh ra phan cai dat (implementation) luc chay -> ta khong phai viet SQL/JPQL thu cong.</p>
 *
 * <p>JpaRepository&lt;OrderEntity, Long&gt; nghia la: lam viec voi entity OrderEntity co khoa
 * chinh (primary key) kieu Long. Da co san save(), findById(), findAll(), delete()...</p>
 *
 * <p><b>Derived query</b> (truy van suy dien tu ten method): Spring doc TEN method va tu sinh
 * cau truy van. Vi du findByStatus -> WHERE status = ?; OrderByCreatedAtDesc -> ORDER BY...</p>
 *
 * <p><b>@EntityGraph chong loi N+1</b>: Mac dinh quan he items la LAZY, nen neu lay danh sach
 * don roi lap qua items cua tung don, Hibernate se ban them 1 query cho moi don -> 1 (lay don)
 * + N (lay items moi don) = van de N+1 query (N+1 problem), rat cham. {@code @EntityGraph}
 * yeu cau Hibernate JOIN FETCH luon "items" trong 1 query duy nhat -> nap san items, tranh N+1.</p>
 *
 * Tu khoa: Spring Data JPA, repository, derived query, N+1 problem, EntityGraph, JOIN FETCH, eager loading
 */
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    // Tim cac don theo trang thai. @EntityGraph nap luon "items" trong cung query (tranh N+1)
    // vi caller se doc items de tra ve client.
    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findByStatus(OrderStatus status);

    // Lay TAT CA don, sap xep theo createdAt giam dan (don moi nhat len truoc).
    // Khong can @EntityGraph o day vi day la danh sach tong quat (tuy ngu canh su dung).
    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    // Bảng bếp: đơn chưa pha xong và chưa hủy, cũ nhất lên trước
    // Phan tich ten method (derived query):
    //   - PreparedFalse        -> WHERE prepared = false (chua pha xong)
    //   - And StatusNot        -> AND status <> ? (loai don co status la tham so truyen vao, vd CANCELLED)
    //   - OrderByCreatedAtAsc  -> ORDER BY createdAt ASC (cu nhat len truoc -> bep lam theo thu tu FIFO)
    // @EntityGraph nap san "items" de man bep hien thi mon ma khong gay N+1.
    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findByPreparedFalseAndStatusNotOrderByCreatedAtAsc(OrderStatus status);
}
