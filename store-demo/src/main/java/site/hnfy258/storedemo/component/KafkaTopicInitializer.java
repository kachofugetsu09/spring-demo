package site.hnfy258.storedemo.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class KafkaTopicInitializer implements CommandLineRunner {

    @Autowired
    private KafkaAdmin kafkaAdmin;

    private static final String ARTICLE_LIKE_EVENTS_TOPIC = "article-likes-events";

    @Override
    public void run(String... args) throws Exception {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // 检查 topic 是否存在
            ListTopicsResult listTopics = adminClient.listTopics();
            Set<String> topicNames = listTopics.names().get(10, TimeUnit.SECONDS);
            
            if (!topicNames.contains(ARTICLE_LIKE_EVENTS_TOPIC)) {
                log.info("Topic '{}' does not exist, creating it...", ARTICLE_LIKE_EVENTS_TOPIC);
                
                NewTopic newTopic = new NewTopic(ARTICLE_LIKE_EVENTS_TOPIC, 3, (short) 1);
                adminClient.createTopics(Collections.singletonList(newTopic)).all().get(30, TimeUnit.SECONDS);
                
                log.info("Topic '{}' created successfully", ARTICLE_LIKE_EVENTS_TOPIC);
            } else {
                log.info("Topic '{}' already exists", ARTICLE_LIKE_EVENTS_TOPIC);
            }
        } catch (Exception e) {
            log.error("Error initializing Kafka topics", e);
            // 不抛出异常，让应用继续启动
        }
    }
}