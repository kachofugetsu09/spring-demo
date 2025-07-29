package site.hnfy258.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.UserBehavior;

import java.time.Instant;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
@Service
@Slf4j
public class KafkaProducerService {
    private static final String TOPIC = "user_behavior_logs";
    private final KafkaTemplate<String,String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final Random random = new Random();
    private final String[] actionTypes = {"click", "view", "add_to_cart", "purchase"};
    private long messageCount = 0;
    private Thread producerThread;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    private UserBehavior generateUserBehavior() {
        String userId = "user_" + random.nextInt(1000);
        String itemId = "item_" + random.nextInt(500);
        String actionType = actionTypes[random.nextInt(actionTypes.length)];
        String messageId = System.currentTimeMillis() + "_" + messageCount; // 用于幂等性演示

        return new UserBehavior(userId, itemId, actionType, Instant.now().toEpochMilli(), messageId);
    }

    public void sendUserBehaviorLog(String acksConfig){
        kafkaTemplate.getProducerFactory().updateConfigs(Collections.singletonMap("acks",acksConfig));
        log.info("开始发送用户行为日志，配置acks: {}", acksConfig);

        // 停止之前的线程（如果存在）
        stopProducer();

        producerThread = new Thread(()->{
            try{
                while(!Thread.currentThread().isInterrupted()){
                    UserBehavior userBehavior = generateUserBehavior();
                    String key = userBehavior.getUserId();
                    String value = objectMapper.writeValueAsString(userBehavior);

                    CompletableFuture<SendResult<String,String>> future = kafkaTemplate.send(TOPIC, key, value);

                    future.whenComplete((result,ex)->{
                        if (ex == null) {
                            if (messageCount % 100 == 0) {
                                log.info("Sent message: topic={}, partition={}, offset={}, key={}, messageId={}",
                                        result.getRecordMetadata().topic(),
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset(),
                                        key, userBehavior.getMessageId());
                            }
                        } else {
                            log.error("Error sending message: {}", ex.getMessage(), ex);
                        }
                    });
                    messageCount++;
                    if (messageCount % 100 == 0) {
                        log.info("Sent {} messages.", messageCount);
                    }

                    TimeUnit.MILLISECONDS.sleep(100);
                }
            }catch (InterruptedException e){
                log.info("Producer thread interrupted, stopping...");
                Thread.currentThread().interrupt();
            }catch (Exception e){
                log.error("Error in Kafka producer thread: {}", e.getMessage(), e);
            }
        });
        producerThread.start();
    }

    public void stopProducer() {
        if (producerThread != null && producerThread.isAlive()) {
            producerThread.interrupt();
            try {
                producerThread.join(1000); // 等待最多1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void sendSingleMessage(String acksConfig) {
        try {
            kafkaTemplate.getProducerFactory().updateConfigs(Collections.singletonMap("acks", acksConfig));
            
            UserBehavior userBehavior = generateUserBehavior();
            String key = userBehavior.getUserId();
            String value = objectMapper.writeValueAsString(userBehavior);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC, key, value);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Single message sent: topic={}, partition={}, offset={}, key={}, messageId={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            key, userBehavior.getMessageId());
                } else {
                    log.error("Error sending single message: {}", ex.getMessage(), ex);
                }
            });
            
            messageCount++;
        } catch (Exception e) {
            log.error("Error sending single message: {}", e.getMessage(), e);
        }
    }




}
