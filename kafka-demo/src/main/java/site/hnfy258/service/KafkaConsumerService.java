package site.hnfy258.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.UserBehavior;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class KafkaConsumerService {
    private final ObjectMapper objectMapper;
    private final RedisStorageService redisStorageService;
    // 消息计数器
    private final AtomicLong autoCommitReceivedCount = new AtomicLong(0);
    private final AtomicLong manualCommitReceivedCount = new AtomicLong(0);

    public KafkaConsumerService(ObjectMapper objectMapper, RedisStorageService redisStorageService) {
        this.objectMapper = objectMapper;
        this.redisStorageService = redisStorageService;
    }

    @KafkaListener(topics = "user_behavior_logs", groupId = "user_behavior_group_springboot", containerFactory = "kafkaListenerContainerFactory")
    public void listenAutoCommit(ConsumerRecord<String, String> record) {
        try {
            long currentCount = autoCommitReceivedCount.incrementAndGet();



            // 解析消息
            UserBehavior userBehavior = objectMapper.readValue(record.value(), UserBehavior.class);

            // 存储到Redis（内部已包含幂等性检查）
            redisStorageService.storeUserBehavior(userBehavior);

            // 日志输出
            if (currentCount % 100 == 0) {
//                log.info("Auto-commit Consumer received {} messages. Last message ID: {}", currentCount, userBehavior.getMessageId());
            }

        } catch (Exception e) {
            log.error("Error processing message in auto-commit consumer: {}", e.getMessage(), e);
        }
    }


    /**
     * 手动提交偏移量的消费者
     * 注意：在 application.properties 中需要配置 spring.kafka.listener.ack-mode=MANUAL 或 BATCH
     * 这里我们使用 BATCH 模式，并手动 ack
     */
    @KafkaListener(topics = "user_behavior_logs", groupId = "user_behavior_group_springboot_manual", containerFactory = "manualAckKafkaListenerContainerFactory")
    public void listenManualCommit(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        try {
            if (!records.isEmpty()) {
                log.info("Manual-commit Consumer received {} records. First offset: {}", records.size(), records.get(0).offset());
                
                for (ConsumerRecord<String, String> record : records) {
                    manualCommitReceivedCount.incrementAndGet();
                    UserBehavior userBehavior = objectMapper.readValue(record.value(), UserBehavior.class);
                    
                    // 存储到Redis（内部已包含幂等性检查）
                    redisStorageService.storeUserBehavior(userBehavior);
                }

                // 批量处理完所有消息后，手动提交偏移量
                acknowledgment.acknowledge();
                log.info("Manual-commit Consumer: Acknowledged {} records. Last offset: {}", records.size(), records.get(records.size()-1).offset());
                //模拟消息积压
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            log.error("Error processing message in manual-commit consumer: {}", e.getMessage(), e);
            // 如果处理失败，不调用 acknowledgment.acknowledge()，消息会在下次 poll 时重新被拉取
        }
    }
    
    /**
     * 获取消费统计信息
     */
    public String getConsumerStats() {
        return String.format("Auto-commit Consumer: %d messages, Manual-commit Consumer: %d messages", 
                           autoCommitReceivedCount.get(), manualCommitReceivedCount.get());
    }
}