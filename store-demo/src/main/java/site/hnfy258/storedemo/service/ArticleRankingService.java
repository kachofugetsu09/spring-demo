package site.hnfy258.storedemo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import site.hnfy258.storedemo.dto.ArticleRankingItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class ArticleRankingService {

    private static final String RANKING_KEY_PREFIX = "article:ranking:";
    private static final String CURRENT_RANKING_KEY = "article:ranking:current";
    
    private final RedisTemplate<String, Object> redisTemplate;

    public ArticleRankingService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 更新文章点赞数量到Redis排行榜
     */
    public void updateArticleLikeCount(Long articleId, Long likeCount, String timeWindow) {
        try {
            // 使用时间窗口作为排行榜的key
            String rankingKey = RANKING_KEY_PREFIX + timeWindow.replace(" ", "_").replace(":", "-");
            
            log.info("Updating Redis ranking: articleId={}, likeCount={}, timeWindow={}, rankingKey={}", 
                    articleId, likeCount, timeWindow, rankingKey);
            
            // 将文章ID和点赞数存储到有序集合中（分数为点赞数）
            redisTemplate.opsForZSet().add(rankingKey, articleId.toString(), likeCount.doubleValue());
            
            // 同时更新当前排行榜
            redisTemplate.opsForZSet().add(CURRENT_RANKING_KEY, articleId.toString(), likeCount.doubleValue());
            
            // 设置过期时间（24小时）
            redisTemplate.expire(rankingKey, java.time.Duration.ofHours(24));
            
            log.info("Successfully updated article {} like count to {} in ranking {}", articleId, likeCount, rankingKey);
            
            // 验证数据是否存储成功
            Double score = redisTemplate.opsForZSet().score(CURRENT_RANKING_KEY, articleId.toString());
            log.info("Verification: article {} current score in Redis: {}", articleId, score);
            
        } catch (Exception e) {
            log.error("Error updating article ranking for article: {}", articleId, e);
        }
    }

    /**
     * 获取当前热门文章排行榜（前N名）
     */
    public List<ArticleRankingItem> getCurrentRanking(int topN) {
        try {
            // 按分数降序获取前N名
            Set<ZSetOperations.TypedTuple<Object>> topArticles = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(CURRENT_RANKING_KEY, 0, topN - 1);
            
            List<ArticleRankingItem> result = new ArrayList<>();
            int rank = 1;
            
            if (topArticles != null) {
                for (ZSetOperations.TypedTuple<Object> tuple : topArticles) {
                    String articleId = tuple.getValue().toString();
                    Long likeCount = tuple.getScore() != null ? tuple.getScore().longValue() : 0L;
                    
                    result.add(new ArticleRankingItem(
                            Long.parseLong(articleId),
                            likeCount,
                            rank++,
                            "current"

                    ));
                }
            }
            
            log.info("Retrieved top {} articles from current ranking", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("Error getting current top articles", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取指定时间窗口的热门文章排行榜
     */
    public List<ArticleRankingItem> getRankingByTimeWindow(String timeWindow, int topN) {
        try {
            String rankingKey = RANKING_KEY_PREFIX + timeWindow.replace(" ", "_").replace(":", "-");
            
            Set<ZSetOperations.TypedTuple<Object>> topArticles = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(rankingKey, 0, topN - 1);
            
            List<ArticleRankingItem> result = new ArrayList<>();
            int rank = 1;
            
            if (topArticles != null) {
                for (ZSetOperations.TypedTuple<Object> tuple : topArticles) {
                    String articleId = tuple.getValue().toString();
                    Long likeCount = tuple.getScore() != null ? tuple.getScore().longValue() : 0L;
                    
                    result.add(new ArticleRankingItem(
                            Long.parseLong(articleId),
                            likeCount,
                            rank++,
                            timeWindow
                    ));
                }
            }
            
            log.info("Retrieved top {} articles from time window: {}", result.size(), timeWindow);
            return result;
            
        } catch (Exception e) {
            log.error("Error getting top articles for time window: {}", timeWindow, e);
            return new ArrayList<>();
        }
    }

    /**
     * 清除指定时间窗口的排行榜数据
     */
    public void clearRankingByTimeWindow(String timeWindow) {
        try {
            String rankingKey = RANKING_KEY_PREFIX + timeWindow.replace(" ", "_").replace(":", "-");
            redisTemplate.delete(rankingKey);
            log.info("Cleared ranking data for time window: {}", timeWindow);
        } catch (Exception e) {
            log.error("Error clearing ranking for time window: {}", timeWindow, e);
        }
    }

    /**
     * 获取文章在当前排行榜中的排名
     */
    public Long getArticleRank(Long articleId) {
        try {
            Long rank = redisTemplate.opsForZSet().reverseRank(CURRENT_RANKING_KEY, articleId.toString());
            return rank != null ? rank + 1 : null; // Redis排名从0开始，转换为从1开始
        } catch (Exception e) {
            log.error("Error getting rank for article: {}", articleId, e);
            return null;
        }
    }
}
