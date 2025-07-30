package site.hnfy258.storedemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;
import site.hnfy258.storedemo.dto.ArticleLikeEvent;
import site.hnfy258.storedemo.service.ArticleRankingService;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Configuration
@EnableKafkaStreams
@Slf4j
public class ArticleLikeStreamsConfig {

    private static final String INPUT_TOPIC = "article-like-events";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;
    private final ArticleRankingService articleRankingService;

    public ArticleLikeStreamsConfig(ObjectMapper objectMapper, ArticleRankingService articleRankingService) {
        this.objectMapper = objectMapper;
        this.articleRankingService = articleRankingService;
    }

    @Bean
    public KStream<String, String> articleLikeStream(StreamsBuilder streamsBuilder) {
        log.info("Initializing Article Like Kafka Streams topology...");
        
        try {
            JsonSerde<ArticleLikeEvent> likeEventSerde = new JsonSerde<>(ArticleLikeEvent.class, objectMapper);

            // 1. 从输入Topic读取点赞事件
            KStream<String, String> sourceStream = streamsBuilder.stream(
                    INPUT_TOPIC,
                    Consumed.with(Serdes.String(), Serdes.String()));

            log.info("Created source stream for topic: {}", INPUT_TOPIC);

            // 2. 添加日志来监控消息接收
            sourceStream.foreach((key, value) -> {
                log.info("Received message from Kafka: key={}, value={}", key, value);
            });

            // 3. 反序列化为ArticleLikeEvent对象
            KStream<String, ArticleLikeEvent> likeEventStream = sourceStream.mapValues((key, value) -> {
                try {
                    if (value == null || value.trim().isEmpty()) {
                        log.warn("Received null or empty value for key: {}", key);
                        return null;
                    }
                    ArticleLikeEvent event = objectMapper.readValue(value, ArticleLikeEvent.class);
                    log.info("Successfully deserialized ArticleLikeEvent: articleId={}, userId={}, action={}", 
                            event.getArticleId(), event.getUserId(), event.getAction());
                    return event;
                } catch (Exception e) {
                    log.error("Error deserializing ArticleLikeEvent for key: {}, value: {}", key, value, e);
                    return null;
                }
            }).filter((key, event) -> {
                boolean isValid = event != null && event.getArticleId() != null;
                if (!isValid) {
                    log.warn("Filtered out invalid event: key={}", key);
                }
                return isValid;
            });

            // 4. 过滤出LIKE事件（忽略UNLIKE）
            KStream<String, ArticleLikeEvent> likeStream = likeEventStream
                    .filter((key, event) -> {
                        boolean isLike = "LIKE".equals(event.getAction());
                        log.info("Filtering event: articleId={}, action={}, isLike={}", 
                                event.getArticleId(), event.getAction(), isLike);
                        return isLike;
                    });

            // 5. 按文章ID分组，设置5秒时间窗口进行聚合
            KStream<String, Long> articleLikeCounts = likeStream
                    .map((key, event) -> {
                        String newKey = event.getArticleId().toString();
                        log.info("Mapping event to new key: oldKey={}, newKey={}, articleId={}", 
                                key, newKey, event.getArticleId());
                        return new KeyValue<>(newKey, event);
                    })
                    .groupByKey(Grouped.with(Serdes.String(), likeEventSerde))
                    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(5)))
                    .count(Materialized.as("article-like-counts-store"))
                    .toStream()
                    .map((windowedKey, count) -> {
                        String articleId = windowedKey.key();
                        long windowStart = windowedKey.window().start();
                        long windowEnd = windowedKey.window().end();
                        
                        // 格式化时间窗口
                        LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(windowStart), ZoneId.systemDefault());
                        LocalDateTime endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(windowEnd), ZoneId.systemDefault());
                        String timeWindow = startTime.format(TIME_FORMATTER) + " - " + endTime.format(TIME_FORMATTER);
                        
                        log.info("Window aggregation result: Article {} like count: {} in window: {}", 
                                articleId, count, timeWindow);
                        
                        // 存储到Redis
                        try {
                            articleRankingService.updateArticleLikeCount(Long.parseLong(articleId), count, timeWindow);
                            log.info("Successfully updated Redis ranking for article: {}", articleId);
                        } catch (Exception e) {
                            log.error("Error updating article ranking for article: {}", articleId, e);
                        }
                        
                        return new KeyValue<>(articleId, count);
                    });

            // 6. 添加最终输出日志
            articleLikeCounts.foreach((articleId, count) -> {
                log.info("Final output: Article {} has {} likes", articleId, count);
            });

            log.info("Article Like Kafka Streams topology initialized successfully");
            return sourceStream;
            
        } catch (Exception e) {
            log.error("Error initializing Kafka Streams topology", e);
            throw e;
        }
    }
}