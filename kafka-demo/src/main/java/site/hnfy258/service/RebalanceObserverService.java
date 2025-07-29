package site.hnfy258.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka再平衡观察服务
 * 用于监控和记录Kafka消费者组的再平衡过程
 */
@Service
@Slf4j
public class RebalanceObserverService implements ConsumerRebalanceListener {
    
    private final AtomicInteger rebalanceCount = new AtomicInteger(0);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        int currentRebalance = rebalanceCount.incrementAndGet();
        String timestamp = LocalDateTime.now().format(formatter);
        
        log.warn("=== 再平衡开始 #{} - {} ===", currentRebalance, timestamp);
        log.warn("📤 分区被撤销 (Partitions Revoked):");
        
        if (partitions.isEmpty()) {
            log.warn("   ❌ 没有分区被撤销");
        } else {
            for (TopicPartition partition : partitions) {
                log.warn("   ❌ Topic: {}, Partition: {}", 
                    partition.topic(), partition.partition());
            }
        }
        
        log.warn("🔄 消费者正在释放分区所有权...");
        
        // 模拟一些清理工作
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        String timestamp = LocalDateTime.now().format(formatter);
        
        log.info("📥 分区被分配 (Partitions Assigned) - {}:", timestamp);
        
        if (partitions.isEmpty()) {
            log.info("   ✅ 没有分区被分配给当前消费者");
        } else {
            for (TopicPartition partition : partitions) {
                log.info("   ✅ Topic: {}, Partition: {} -> 当前消费者", 
                    partition.topic(), partition.partition());
            }
        }
        
        log.info("=== 再平衡完成 #{} - {} ===", 
            rebalanceCount.get(), timestamp);
        log.info("🎉 消费者现在拥有 {} 个分区", partitions.size());
        
        // 打印分区分配摘要
        printPartitionSummary(partitions);
    }
    
    private void printPartitionSummary(Collection<TopicPartition> partitions) {
        log.info("📊 当前分区分配摘要:");
        log.info("   总分区数: {}", partitions.size());
        
        partitions.stream()
            .collect(java.util.stream.Collectors.groupingBy(TopicPartition::topic))
            .forEach((topic, topicPartitions) -> {
                String partitionNumbers = topicPartitions.stream()
                    .map(tp -> String.valueOf(tp.partition()))
                    .collect(java.util.stream.Collectors.joining(", "));
                log.info("   Topic '{}': 分区 [{}]", topic, partitionNumbers);
            });
    }
    
    public int getRebalanceCount() {
        return rebalanceCount.get();
    }
    
    public void logCurrentStatus() {
        log.info("🔍 再平衡统计: 总共发生了 {} 次再平衡", rebalanceCount.get());
    }
}