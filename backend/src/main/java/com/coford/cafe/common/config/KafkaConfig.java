package com.coford.cafe.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Cau hinh Apache Kafka cho ung dung — khai bao topic dung de phat su kien don hang.
 *
 * <p>Vai tro trong kien truc: Kafka la mot nen tang truyen tin nhan/su kien theo mo hinh
 * publish-subscribe (xuat ban - dang ky nhan). Cac thanh phan trong he thong giao tiep voi nhau
 * mot cach loi long (loose coupling) qua su kien thay vi goi truc tiep: ben gui (producer) day
 * su kien vao topic, ben nhan (consumer) lang nghe topic do (xem cac lop dung @KafkaListener).
 *
 * <p>Khai niem minh hoa:
 * <ul>
 *   <li>Topic: "kenh" chua cac su kien cung loai (o day la su kien lien quan den don hang).</li>
 *   <li>Partition: topic duoc chia thanh nhieu phan de xu ly song song; cung key se vao cung partition,
 *       giup giu thu tu theo key.</li>
 *   <li>Replica: ban sao cua du lieu tren nhieu broker de chong mat mat (do ben - durability).</li>
 *   <li>Event-driven architecture: kien truc huong su kien.</li>
 * </ul>
 *
 * <p>Tu khoa: Apache Kafka, Spring Kafka, topic, partition, replica, producer, consumer,
 * @KafkaListener, event-driven architecture, publish-subscribe.
 */
// @Configuration: lop cau hinh khai bao bean.
@Configuration
public class KafkaConfig {

    // Ten topic dung chung cho ca producer va consumer. Tach thanh hang so de tranh go sai chinh ta.
    public static final String ORDER_EVENTS = "coford.order-events";

    // @Bean tra ve NewTopic: khi ung dung khoi dong, Spring Kafka se TU DONG TAO topic nay tren broker
    //   neu chua ton tai (qua KafkaAdmin). Day la cach khai bao topic bang code thay vi tao thu cong.
    @Bean
    public NewTopic orderEventsTopic() {
        // TopicBuilder: builder de cau hinh topic.
        //   partitions(1): topic co 1 partition (du cho moi truong hoc tap; production thuong nhieu hon).
        //   replicas(1): chi 1 ban sao (phu hop cluster 1 broker; production nen >= 3 de chiu loi).
        return TopicBuilder.name(ORDER_EVENTS).partitions(1).replicas(1).build();
    }
}
