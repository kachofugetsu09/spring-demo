#!/bin/bash

# 快速再平衡演示脚本
# 简化版本，专注于观察再平衡过程

echo "🔄 Kafka再平衡快速演示"
echo "======================"

# 配置
BOOTSTRAP_SERVERS="localhost:9092"
TOPIC_NAME="user_behavior_logs"
GROUP_ID="rebalance_demo_group"

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Topic: $TOPIC_NAME${NC}"
echo -e "${BLUE}Consumer Group: $GROUP_ID${NC}"
echo ""

# 检查topic分区数
echo -e "${GREEN}1. 检查topic分区信息:${NC}"
kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS --describe --topic $TOPIC_NAME 2>/dev/null || {
    echo "❌ Topic不存在或Kafka未运行"
    exit 1
}

echo ""
echo -e "${GREEN}2. 当前消费者组状态:${NC}"
kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS --describe --group $GROUP_ID 2>/dev/null || echo "消费者组暂无活跃消费者"

echo ""
echo -e "${YELLOW}3. 演示步骤:${NC}"
echo "   a) 启动第一个应用: mvn spring-boot:run"
echo "   b) 启动第二个应用: mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081"
echo "   c) 观察日志中的再平衡过程"
echo "   d) 停止一个应用，观察再次再平衡"

echo ""
echo -e "${YELLOW}4. 监控命令:${NC}"
echo "   监控消费者组: watch -n 2 'kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS --describe --group $GROUP_ID'"
echo "   查看分区分配: kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS --describe --group $GROUP_ID --members --verbose"

echo ""
echo -e "${YELLOW}5. API查看统计:${NC}"
echo "   curl http://localhost:8080/api/rebalance/stats"
echo "   curl http://localhost:8081/api/rebalance/stats"

echo ""
echo "🎯 关键观察点:"
echo "   - 日志中的 '再平衡开始' 和 '再平衡完成' 消息"
echo "   - 分区被撤销 (Partitions Revoked) 和分配 (Partitions Assigned)"
echo "   - 不同消费者实例之间的分区重新分配"
echo "   - 消费者加入/离开时的自动再平衡"