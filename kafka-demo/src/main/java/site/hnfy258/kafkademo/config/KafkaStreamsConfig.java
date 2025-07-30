package site.hnfy258.kafkademo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde; // Spring Kafka 提供的 JSON Serde

import site.hnfy258.entity.ItemViewStats;
import site.hnfy258.entity.UserBehavior;

import java.time.Duration;

@Configuration
@EnableKafkaStreams // 启用 Spring Kafka Streams
@Slf4j
public class KafkaStreamsConfig {

    private static final String INPUT_TOPIC = "user_behavior_logs";
    private static final String OUTPUT_TOPIC = "realtime_item_views";

    private final ObjectMapper objectMapper;

    // Spring Boot 会自动注入 ObjectMapper
    public KafkaStreamsConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("KafkaStreamsConfig initialized with ObjectMapper: {}", objectMapper != null ? "OK" : "NULL");
    }

    /**
     * 定义 Kafka Streams 的处理拓扑
     * 
     * @param streamsBuilder Spring Kafka 会自动提供一个 StreamsBuilder bean
     * @return KStream 拓扑
     */
    @Bean
    public KStream<String, String> kStream(StreamsBuilder streamsBuilder) {
        // 检查 streamsBuilder 是否为 null
        if (streamsBuilder == null) {
            log.error("StreamsBuilder is null, cannot create KStream topology");
            throw new IllegalArgumentException("StreamsBuilder cannot be null");
        }

        // 定义 JSON 序列化和反序列化器，用于 UserBehavior 和 ItemViewStats 对象
        JsonSerde<UserBehavior> userBehaviorJsonSerde = new JsonSerde<>(UserBehavior.class, objectMapper);
        JsonSerde<ItemViewStats> itemViewStatsJsonSerde = new JsonSerde<>(ItemViewStats.class, objectMapper);

        // 1. 从输入 Topic 读取消息
        KStream<String, String> sourceStream = streamsBuilder.stream(
                INPUT_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String()));

        // 2. 将 JSON 字符串 Value 反序列化为 UserBehavior 对象
        KStream<String, UserBehavior> userBehaviorStream = sourceStream.mapValues((key, value) -> {
            try {
                if (value == null || value.trim().isEmpty()) {
                    log.warn("Received null or empty value for key: {}", key);
                    return null;
                }
                UserBehavior behavior = objectMapper.readValue(value, UserBehavior.class);
                log.debug("Successfully deserialized UserBehavior: {}", behavior);
                return behavior;
            } catch (Exception e) {
                log.error("Error deserializing UserBehavior for key: {}, value: {}", key, value, e);
                return null; // 无法反序列化的消息可以跳过或特殊处理
            }
        }).filter((key, userBehavior) -> {
            boolean isValid = userBehavior != null && userBehavior.getItemId() != null
                    && userBehavior.getActionType() != null;
            if (!isValid && userBehavior != null) {
                log.warn("Filtering out invalid UserBehavior: {}", userBehavior);
            }
            return isValid;
        }); // 过滤掉反序列化失败的 null 值和无效数据

        // 3. 过滤出 'view' 类型的行为
        KStream<String, UserBehavior> viewStream = userBehaviorStream
                .filter((key, userBehavior) -> "view".equalsIgnoreCase(userBehavior.getActionType()));

        // 4. 按 item_id 分组，并设置时间窗口（例如每 5 秒的浏览量）
        KStream<String, ItemViewStats> itemViews = viewStream
                .map((key, userBehavior) -> new KeyValue<>(userBehavior.getItemId(),
                        userBehavior)) // 将 item_id 作为新的 Key
                .groupByKey(Grouped.with(Serdes.String(), userBehaviorJsonSerde)) // 按 item_id 分组，指定序列化器
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(5))) // 设置 5 秒的滚动时间窗口
                .count(Materialized.as("item-view-counts-store")) // 计数
                .toStream() // 将 KTable 转换回 KStream
                .map((windowedKey, count) -> {
                    String itemId = windowedKey.key();
                    long windowStart = windowedKey.window().start();
                    long windowEnd = windowedKey.window().end();
                    return new KeyValue<>(itemId,
                            new ItemViewStats(itemId, count, windowStart, windowEnd));
                });

        // 5. 将结果输出到新的 Topic
        itemViews.to(
                OUTPUT_TOPIC,
                // 使用 StringSerde 作为 Key，ItemViewStatsJsonSerde 作为 Value 的序列化器
                Produced.with(Serdes.String(), itemViewStatsJsonSerde));

        log.info("Kafka Streams topology defined for Item View Counts.");
        return sourceStream; // 返回任意 KStream 即可
    }
}