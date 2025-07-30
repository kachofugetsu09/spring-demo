# 文章点赞系统测试指南

## 问题修复

### Jackson LocalDateTime 序列化问题

**问题描述：**
```
Java 8 date/time type `java.time.LocalDateTime` not supported by default: 
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
```

**解决方案：**

1. **添加依赖** (已完成)
   ```xml
   <dependency>
       <groupId>com.fasterxml.jackson.datatype</groupId>
       <artifactId>jackson-datatype-jsr310</artifactId>
   </dependency>
   ```

2. **配置ObjectMapper** (已完成)
   - 创建了 `JacksonConfig.java` 配置类
   - 注册了 `JavaTimeModule`
   - 禁用了时间戳格式

3. **配置application.properties** (已完成)
   ```properties
   spring.jackson.serialization.write-dates-as-timestamps=false
   spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
   spring.jackson.time-zone=GMT+8
   ```

4. **增强DTO注解** (已完成)
   ```java
   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   @JsonSerialize(using = LocalDateTimeSerializer.class)
   @JsonDeserialize(using = LocalDateTimeDeserializer.class)
   private LocalDateTime timestamp;
   ```

### MyBatis SqlSession 优化

**问题描述：**
频繁创建和关闭非事务SqlSession，导致大量日志输出。

**解决方案：**

1. **添加只读事务注解** (已完成)
   ```java
   @Transactional(readOnly = true)
   public Long getArticleLikeCount(Long articleId) { ... }
   ```

2. **添加批量查询接口** (已完成)
   ```java
   @GetMapping("/likes/batch-count")
   public ResponseEntity<Map<String, Object>> getBatchArticleLikeCounts(...)
   ```

## 测试页面功能

### 访问地址
```
http://localhost:8081/article-like-test.html
```

### 主要功能

1. **文章列表显示**
   - 显示5篇预设文章
   - 实时显示每篇文章的点赞数
   - 每10秒自动刷新点赞数

2. **点赞测试**
   - 🎲 随机点赞：随机选择一篇文章进行点赞
   - ❤️ 点赞文章：点击具体文章的点赞按钮

3. **Kafka测试**
   - 🧪 测试Kafka：发送测试消息到Kafka（不保存到数据库）
   - 验证Jackson序列化是否正常工作

4. **排行榜功能**
   - 🔄 刷新排行榜：查看Kafka Streams处理后的排行榜数据
   - 验证Redis缓存是否正常工作

5. **自动测试**
   - 可配置测试次数和间隔时间
   - 批量发送点赞请求进行压力测试

6. **实时统计**
   - 总点赞数
   - 成功率
   - 平均响应时间

## API接口

### 点赞相关
- `POST /api/articles/{articleId}/like` - 点赞文章
- `GET /api/articles/{articleId}/likes/count` - 获取文章点赞数
- `GET /api/articles/likes/batch-count?articleIds=1,2,3,4,5` - 批量获取点赞数

### 测试相关
- `POST /api/articles/{articleId}/test-kafka` - 测试Kafka消息发送

### 排行榜相关
- `GET /api/articles/ranking?limit=10` - 获取文章排行榜

## 验证步骤

1. **启动应用**
   ```bash
   cd store-demo
   mvn spring-boot:run
   ```

2. **确保依赖服务运行**
   - MySQL (端口3306)
   - Redis (端口6379)
   - Kafka (端口9092)

3. **访问测试页面**
   ```
   http://localhost:8081/article-like-test.html
   ```

4. **测试流程**
   - 点击"测试Kafka"验证序列化问题是否修复
   - 点击"随机点赞"测试完整流程
   - 点击"刷新排行榜"验证Kafka Streams处理
   - 使用自动测试进行批量验证

## 预期结果

1. **Jackson序列化正常**
   - 不再出现LocalDateTime序列化错误
   - Kafka消息发送成功

2. **数据库操作优化**
   - SqlSession日志减少
   - 查询性能提升

3. **完整流程验证**
   - 点赞 → 数据库保存 → Kafka消息 → Streams处理 → Redis缓存 → 排行榜显示

## 故障排除

### 如果仍然出现序列化错误
1. 检查依赖是否正确添加
2. 重启应用确保配置生效
3. 查看启动日志确认JavaTimeModule注册成功

### 如果Kafka连接失败
1. 确认Kafka服务运行状态
2. 检查application.properties中的Kafka配置
3. 查看Kafka日志

### 如果排行榜为空
1. 确认Kafka Streams应用正常启动
2. 检查Redis连接状态
3. 验证topic是否创建成功