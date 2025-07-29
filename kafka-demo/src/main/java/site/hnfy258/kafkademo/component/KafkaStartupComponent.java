package site.hnfy258.kafkademo.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import site.hnfy258.service.KafkaProducerService;

@Component
@Slf4j
public class KafkaStartupComponent implements ApplicationRunner {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== Kafka Demo Application Started ===");
        log.info("应用启动完成，Kafka消息发送服务已就绪");
        
        log.info("可以通过以下API控制:");
        log.info("- POST /kafka/start?acks=all  - 开始发送消息");
        log.info("- POST /kafka/stop            - 停止发送消息");
        log.info("- GET  /kafka/status          - 查看状态");
        log.info("- POST /kafka/send-single     - 发送单条消息");
        log.info("- 访问 http://localhost:8080 打开控制台");
    }
}