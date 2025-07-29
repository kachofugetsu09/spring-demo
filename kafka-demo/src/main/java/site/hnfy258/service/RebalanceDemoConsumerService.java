package site.hnfy258.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 再平衡演示消费者服务
 * 用于演示多个消费者实例之间的再平衡过程
 */
@Service
@Slf4j
public class RebalanceDemoConsumerService {
    
    @Autowired
    private RebalanceObserverService rebalanceObserver;
    
    private final AtomicLong messageCount = new AtomicLong(0);
    private final String consumerInstanceId = "Consumer-" + System.currentTimeMillis() % 10000;
    
    @KafkaListener(
        topics = "user_behavior_logs", 
        groupId = "rebalance_demo_group",
        containerFactory = "rebalanceDemoKafkaListenerContainerFactory"
    )
    public void consumeWithRebalanceMonitoring(ConsumerRecord<String, String> record) {
        long count = messageCount.incrementAndGet();
        
        // 每处理100条消息输出一次状态
        if (count % 100 == 0) {
            log.info("🔥 {} 处理了第 {} 条消息 | Partition: {} | Offset: {}", 
                consumerInstanceId, count, record.partition(), record.offset());
            rebalanceObserver.logCurrentStatus();
        }
        
        // 模拟消息处理时间
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public String getConsumerStats() {
        return String.format("%s: 已处理 %d 条消息, 发生 %d 次再平衡", 
            consumerInstanceId, messageCount.get(), rebalanceObserver.getRebalanceCount());
    }
    
    public String getConsumerInstanceId() {
        return consumerInstanceId;
    }
}