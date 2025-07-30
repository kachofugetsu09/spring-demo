package site.hnfy258.storedemo.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class KafkaStreamsHealthCheck implements ApplicationRunner {

    @Autowired(required = false)
    private StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Wait a bit for KafkaStreams to fully initialize
        Thread.sleep(2000);
        checkKafkaStreamsHealth();
    }

    public void checkKafkaStreamsHealth() {
        if (streamsBuilderFactoryBean == null) {
            log.error("StreamsBuilderFactoryBean is null - Kafka Streams not properly configured");
            return;
        }

        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            if (kafkaStreams == null) {
                log.warn("KafkaStreams instance is null - may not be started yet");
            } else {
                log.info("KafkaStreams state: {}", kafkaStreams.state());
                log.info("KafkaStreams health check completed successfully");
            }
        } catch (Exception e) {
            log.error("Error checking Kafka Streams health", e);
        }
    }
}