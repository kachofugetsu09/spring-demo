package site.hnfy258.storedemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import site.hnfy258.storedemo.dto.ArticleLikeEvent;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ArticleLikeProducerService {

    private static final String TOPIC = "article-likes-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ArticleLikeProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送文章点赞事件到Kafka
     */
    public void sendArticleLikeEvent(Long articleId, Long userId, String action) {
        try {
            ArticleLikeEvent event = new ArticleLikeEvent(
                    articleId,
                    userId,
                    action,
                    new Date());

            String eventJson = objectMapper.writeValueAsString(event);
            String key = articleId.toString(); // 使用文章ID作为key，确保同一文章的事件有序

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC, key, eventJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info(
                            "Article like event sent successfully: topic={}, partition={}, offset={}, articleId={}, userId={}, action={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            articleId, userId, action);
                } else {
                    log.error("Failed to send article like event: articleId={}, userId={}, action={}, error={}",
                            articleId, userId, action, ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error creating article like event: articleId={}, userId={}, action={}",
                    articleId, userId, action, e);
            throw new RuntimeException("发送点赞事件失败", e);
        }
    }

    /**
     * 批量发送文章点赞事件（用于测试）
     */
    public void sendBatchArticleLikeEvents(Long articleId, int count) {
        log.info("开始批量发送文章点赞事件: articleId={}, count={}", articleId, count);

        for (int i = 0; i < count; i++) {
            Long userId = (long) (Math.random() * 10000 + 1);
            sendArticleLikeEvent(articleId, userId, "LIKE");

            // 避免发送过快
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("批量发送完成: articleId={}, count={}", articleId, count);
    }

    /**
     * 发送取消点赞事件
     */
    public void sendArticleUnlikeEvent(Long articleId, Long userId) {
        sendArticleLikeEvent(articleId, userId, "UNLIKE");
    }
    
    /**
     * 测试发送点赞事件（用于调试）
     */
    public void sendTestLikeEvent(Long articleId) {
        Long userId = (long) (Math.random() * 10000 + 1);
        log.info("Sending test like event: articleId={}, userId={}", articleId, userId);
        sendArticleLikeEvent(articleId, userId, "LIKE");
    }
}