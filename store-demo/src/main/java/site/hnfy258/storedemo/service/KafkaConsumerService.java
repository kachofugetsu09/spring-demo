package site.hnfy258.storedemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import site.hnfy258.storedemo.dto.ArticleLikeEvent;

import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class KafkaConsumerService {
    private final ObjectMapper objectMapper;
    // 消息计数器
    private final AtomicLong articleLikeReceivedCount = new AtomicLong(0);

    public KafkaConsumerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 消费文章点赞事件（用于监控和日志记录）
     */
    @KafkaListener(topics = "article_likes_events", groupId = "article-like-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void listenArticleLikeEvents(ConsumerRecord<String, String> record) {
        try {
            long currentCount = articleLikeReceivedCount.incrementAndGet();
            
            // 解析消息
            ArticleLikeEvent event = objectMapper.readValue(record.value(), ArticleLikeEvent.class);
            
            // 记录日志
            log.info("Received article like event: articleId={}, userId={}, action={}, timestamp={}, partition={}, offset={}",
                    event.getArticleId(), event.getUserId(), event.getAction(), event.getTimestamp(),
                    record.partition(), record.offset());
            
            // 这里可以添加其他业务逻辑，比如：
            // 1. 发送通知给文章作者
            // 2. 更新用户行为分析数据
            // 3. 触发推荐算法更新
            
            if (currentCount % 10 == 0) {
                log.info("Article like consumer has processed {} events", currentCount);
            }

        } catch (Exception e) {
            log.error("Error processing article like event: partition={}, offset={}, key={}, value={}", 
                    record.partition(), record.offset(), record.key(), record.value(), e);
        }
    }
    
    /**
     * 获取消费统计信息
     */
    public String getConsumerStats() {
        return String.format("Article Like Consumer: %d events processed", 
                           articleLikeReceivedCount.get());
    }
}