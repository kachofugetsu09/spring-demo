package site.hnfy258.storedemo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.hnfy258.storedemo.entity.ArticleLike;
import site.hnfy258.storedemo.mapper.ArticleLikeMapper;

import java.util.*;

@Service
@Slf4j
public class ArticleLikeService {

    private final Random random = new Random();

    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleLikeProducerService articleLikeProducerService;

    public ArticleLikeService(ArticleLikeMapper articleLikeMapper, 
                            ArticleLikeProducerService articleLikeProducerService) {
        this.articleLikeMapper = articleLikeMapper;
        this.articleLikeProducerService = articleLikeProducerService;
    }

    /**
     * 点赞文章（模拟用户操作）
     */
    @Transactional
    public Long likeArticle(Long articleId) {
        try {
            // 生成随机用户ID（1-10000之间）
            Long userId = (long) (random.nextInt(10000) + 1);
            
            // 检查是否已经点赞过
            QueryWrapper<ArticleLike> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("article_id", articleId)
                       .eq("user_id", userId);
            
            ArticleLike existingLike = articleLikeMapper.selectOne(queryWrapper);
            
            if (existingLike != null) {
                log.info("User {} already liked article {}, generating new userId", userId, articleId);
                // 如果已经点赞，重新生成用户ID
                userId = (long) (random.nextInt(10000) + 10001); // 使用更大的范围避免重复
            }
            
            // 保存点赞记录到数据库
            ArticleLike articleLike = new ArticleLike();
            articleLike.setArticleId(articleId);
            articleLike.setUserId(userId);
            articleLike.setLikeTime(new Date());
            
            articleLikeMapper.insert(articleLike);
            
            // 发送Kafka消息
            articleLikeProducerService.sendArticleLikeEvent(articleId, userId, "LIKE");
            
            log.info("User {} liked article {}, event sent to Kafka", userId, articleId);
            return userId;
            
        } catch (Exception e) {
            log.error("Error processing like for article: {}", articleId, e);
            throw new RuntimeException("点赞失败", e);
        }
    }

    /**
     * 获取文章点赞总数（从数据库）
     */
    @Transactional(readOnly = true)
    public Long getArticleLikeCount(Long articleId) {
        try {
            QueryWrapper<ArticleLike> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("article_id", articleId);
            return articleLikeMapper.selectCount(queryWrapper);
        } catch (Exception e) {
            log.error("Error getting like count for article: {}", articleId, e);
            return 0L;
        }
    }

    /**
     * 批量获取多篇文章的点赞总数（减少数据库连接）
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> getBatchArticleLikeCounts(List<Long> articleIds) {
        try {
            Map<Long, Long> result = new HashMap<>();
            
            // 使用IN查询一次性获取所有数据
            QueryWrapper<ArticleLike> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("article_id", articleIds)
                       .select("article_id", "COUNT(*) as count")
                       .groupBy("article_id");
            
            // 这里需要使用原生SQL或者分别查询，MyBatis-Plus的groupBy不直接支持COUNT
            for (Long articleId : articleIds) {
                QueryWrapper<ArticleLike> wrapper = new QueryWrapper<>();
                wrapper.eq("article_id", articleId);
                Long count = articleLikeMapper.selectCount(wrapper);
                result.put(articleId, count);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error getting batch like counts for articles: {}", articleIds, e);
            return new HashMap<>();
        }
    }

    /**
     * 发送测试Kafka消息（不保存到数据库）
     */
    public void sendTestKafkaMessage(Long articleId, Long userId, String action) {
        try {
            log.info("Sending test Kafka message: articleId={}, userId={}, action={}", articleId, userId, action);
            articleLikeProducerService.sendArticleLikeEvent(articleId, userId, action);
            log.info("Test Kafka message sent successfully");
        } catch (Exception e) {
            log.error("Error sending test Kafka message: articleId={}, userId={}, action={}", articleId, userId, action, e);
            throw new RuntimeException("发送测试Kafka消息失败", e);
        }
    }
    
    /**
     * 发送测试Kafka消息（重载方法，默认LIKE动作）
     */
    public void sendTestKafkaMessage(Long articleId, Long userId) {
        sendTestKafkaMessage(articleId, userId, "LIKE");
    }
}