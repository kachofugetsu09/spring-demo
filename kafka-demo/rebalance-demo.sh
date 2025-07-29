#!/bin/bash

# Kafka再平衡演示脚本
# 用于演示Kafka消费者组的再平衡机制

echo "🚀 Kafka再平衡演示脚本"
echo "=========================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
KAFKA_HOME=${KAFKA_HOME:-"/opt/kafka"}
BOOTSTRAP_SERVERS=${BOOTSTRAP_SERVERS:-"localhost:9092"}
TOPIC_NAME="user_behavior_logs"
GROUP_ID="rebalance_demo_group"

print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================${NC}"
}

print_info() {
    echo -e "${GREEN}ℹ️  $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 检查Kafka是否运行
check_kafka() {
    print_header "检查Kafka状态"
    
    if ! command -v kafka-topics.sh &> /dev/null; then
        print_error "kafka-topics.sh 命令未找到，请确保Kafka已安装并在PATH中"
        exit 1
    fi
    
    # 尝试列出topics来检查连接
    if kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS --list &> /dev/null; then
        print_info "Kafka服务器连接正常"
    else
        print_error "无法连接到Kafka服务器: $BOOTSTRAP_SERVERS"
        exit 1
    fi
}

# 查看topic信息
show_topic_info() {
    print_header "Topic信息"
    
    print_info "Topic: $TOPIC_NAME"
    kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVERS --describe --topic $TOPIC_NAME
    
    echo ""
    print_info "当前消费者组信息:"
    kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS --describe --group $GROUP_ID 2>/dev/null || print_warning "消费者组 $GROUP_ID 不存在或没有活跃消费者"
}

# 监控消费者组
monitor_consumer_group() {
    print_header "监控消费者组再平衡"
    
    print_info "开始监控消费者组: $GROUP_ID"
    print_info "按 Ctrl+C 停止监控"
    echo ""
    
    while true; do
        clear
        echo -e "${BLUE}=== Kafka消费者组监控 $(date) ===${NC}"
        echo ""
        
        # 显示消费者组详情
        echo -e "${GREEN}消费者组详情:${NC}"
        kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS --describe --group $GROUP_ID 2>/dev/null || echo "消费者组暂无活跃消费者"
        
        echo ""
        echo -e "${GREEN}分区分配情况:${NC}"
        kafka-consumer-groups.sh --bootstrap-server $BOOTSTRAP_SERVERS --describe --group $GROUP_ID --members --verbose 2>/dev/null || echo "无活跃消费者"
        
        echo ""
        echo -e "${YELLOW}提示: 启动/停止应用实例来观察再平衡过程${NC}"
        
        sleep 3
    done
}

# 创建测试消息
produce_test_messages() {
    print_header "发送测试消息"
    
    print_info "向topic $TOPIC_NAME 发送测试消息..."
    
    for i in {1..100}; do
        message="{\"messageId\":\"test-$i\",\"userId\":\"user-$((i%10))\",\"action\":\"rebalance-test\",\"timestamp\":$(date +%s)000}"
        echo "$message" | kafka-console-producer.sh --bootstrap-server $BOOTSTRAP_SERVERS --topic $TOPIC_NAME
        
        if [ $((i % 20)) -eq 0 ]; then
            print_info "已发送 $i 条消息..."
            sleep 1
        fi
    done
    
    print_info "测试消息发送完成"
}

# 显示再平衡演示指南
show_guide() {
    print_header "再平衡演示指南"
    
    echo -e "${GREEN}步骤1: 启动第一个应用实例${NC}"
    echo "cd kafka-demo && mvn spring-boot:run"
    echo ""
    
    echo -e "${GREEN}步骤2: 观察初始分区分配${NC}"
    echo "查看应用日志，观察分区分配情况"
    echo ""
    
    echo -e "${GREEN}步骤3: 启动第二个应用实例${NC}"
    echo "cd kafka-demo && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081"
    echo ""
    
    echo -e "${GREEN}步骤4: 观察再平衡过程${NC}"
    echo "查看两个实例的日志，观察分区重新分配"
    echo ""
    
    echo -e "${GREEN}步骤5: 停止一个实例${NC}"
    echo "停止其中一个实例，观察再次再平衡"
    echo ""
    
    echo -e "${GREEN}步骤6: 使用API查看统计${NC}"
    echo "curl http://localhost:8080/api/rebalance/stats"
    echo "curl http://localhost:8081/api/rebalance/stats"
    echo ""
    
    echo -e "${YELLOW}有用的命令:${NC}"
    echo "- 监控消费者组: $0 monitor"
    echo "- 发送测试消息: $0 produce"
    echo "- 查看topic信息: $0 info"
}

# 主菜单
show_menu() {
    print_header "Kafka再平衡演示工具"
    
    echo "请选择操作:"
    echo "1) 显示演示指南"
    echo "2) 检查Kafka状态"
    echo "3) 查看Topic信息"
    echo "4) 监控消费者组"
    echo "5) 发送测试消息"
    echo "6) 退出"
    echo ""
    read -p "请输入选项 (1-6): " choice
    
    case $choice in
        1) show_guide ;;
        2) check_kafka ;;
        3) show_topic_info ;;
        4) monitor_consumer_group ;;
        5) produce_test_messages ;;
        6) exit 0 ;;
        *) print_error "无效选项" ;;
    esac
}

# 命令行参数处理
case "$1" in
    "guide") show_guide ;;
    "check") check_kafka ;;
    "info") show_topic_info ;;
    "monitor") monitor_consumer_group ;;
    "produce") produce_test_messages ;;
    "") show_menu ;;
    *) 
        echo "用法: $0 [guide|check|info|monitor|produce]"
        echo "或者直接运行 $0 进入交互模式"
        ;;
esac