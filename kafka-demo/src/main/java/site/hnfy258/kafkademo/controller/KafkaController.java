package site.hnfy258.kafkademo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.service.KafkaProducerService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/kafka")
@Slf4j
public class KafkaController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    private Thread producerThread;
    private volatile boolean isProducing = false;

    @PostMapping("/start")
    public Map<String, Object> startProducer(@RequestParam(defaultValue = "all") String acks) {
        Map<String, Object> response = new HashMap<>();
        
        if (isProducing) {
            response.put("status", "error");
            response.put("message", "Producer is already running");
            return response;
        }

        try {
            isProducing = true;
            kafkaProducerService.sendUserBehaviorLog(acks);
            
            response.put("status", "success");
            response.put("message", "Kafka producer started with acks=" + acks);
            log.info("Kafka producer started with acks={}", acks);
        } catch (Exception e) {
            isProducing = false;
            response.put("status", "error");
            response.put("message", "Failed to start producer: " + e.getMessage());
            log.error("Failed to start Kafka producer", e);
        }
        
        return response;
    }

    @PostMapping("/stop")
    public Map<String, Object> stopProducer() {
        Map<String, Object> response = new HashMap<>();
        
        if (!isProducing) {
            response.put("status", "error");
            response.put("message", "Producer is not running");
            return response;
        }

        try {
            isProducing = false;
            kafkaProducerService.stopProducer();
            
            response.put("status", "success");
            response.put("message", "Kafka producer stopped");
            log.info("Kafka producer stopped");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to stop producer: " + e.getMessage());
            log.error("Failed to stop Kafka producer", e);
        }
        
        return response;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("isProducing", isProducing);
        response.put("message", isProducing ? "Producer is running" : "Producer is stopped");
        return response;
    }

    @PostMapping("/send-single")
    public Map<String, Object> sendSingleMessage(@RequestParam(defaultValue = "all") String acks) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            kafkaProducerService.sendSingleMessage(acks);
            
            response.put("status", "success");
            response.put("message", "Single message sent with acks=" + acks);
            log.info("Single message sent with acks={}", acks);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to send message: " + e.getMessage());
            log.error("Failed to send single message", e);
        }
        
        return response;
    }
}