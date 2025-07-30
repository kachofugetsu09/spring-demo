package site.hnfy258.storedemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.storedemo.dto.ArticleRankingItem;
import site.hnfy258.storedemo.service.ArticleLikeService;
import site.hnfy258.storedemo.service.ArticleRankingService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
@Slf4j
public class ArticleController {

    private final ArticleLikeService articleLikeService;
    private final ArticleRankingService articleRankingService;

    public ArticleController(ArticleLikeService articleLikeService,
                             ArticleRankingService articleRankingService) {
        this.articleLikeService = articleLikeService;
        this.articleRankingService = articleRankingService;
    }

    /**
     * 点赞文章接口
     * POST /api/articles/{articleId}/like
     */
    @PostMapping("/{articleId}/like")
    public ResponseEntity<Map<String, Object>> likeArticle(@PathVariable Long articleId) {
        try {
            log.info("Received like request for article: {}", articleId);

            // 调用点赞服务
            Long userId = articleLikeService.likeArticle(articleId);

            // 构造响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("articleId", articleId);
            response.put("message", "点赞成功");
            response.put("timestamp", System.currentTimeMillis());

            log.info("Article {} liked successfully by user {}", articleId, userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error liking article: {}", articleId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("articleId", articleId);
            errorResponse.put("message", "点赞失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 查询文章排行榜接口
     * GET /api/articles/ranking
     */
    @GetMapping("/ranking")
    public ResponseEntity<Map<String, Object>> getArticleRanking(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String timeWindow) {
        try {
            log.info("Received ranking request with limit: {}, timeWindow: {}", limit, timeWindow);

            List<ArticleRankingItem> rankings;
            String actualTimeWindow;

            if (timeWindow != null && !timeWindow.trim().isEmpty()) {
                // 查询指定时间窗口的排行榜
                rankings = articleRankingService.getRankingByTimeWindow(timeWindow, limit);
                actualTimeWindow = timeWindow;
            } else {
                // 查询当前时间窗口的排行榜
                rankings = articleRankingService.getCurrentRanking(limit);
                actualTimeWindow = rankings.isEmpty() ? "暂无数据" : rankings.get(0).getTimeWindow();
            }

            // 构造响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timeWindow", actualTimeWindow);
            response.put("rankings", rankings);
            response.put("total", rankings.size());
            response.put("timestamp", System.currentTimeMillis());

            log.info("Retrieved {} ranking items for window: {}", rankings.size(), actualTimeWindow);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting article ranking", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询排行榜失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

//    /**
//     * 获取可用的时间窗口列表
//     * GET /api/articles/ranking/windows
//     */
//    @GetMapping("/ranking/windows")
//    public ResponseEntity<Map<String, Object>> getAvailableTimeWindows() {
//        try {
//            List<String> timeWindows = articleRankingService.getAvailableTimeWindows();
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("timeWindows", timeWindows);
//            response.put("total", timeWindows.size());
//            response.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("Error getting available time windows", e);
//
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("success", false);
//            errorResponse.put("message", "查询时间窗口失败: " + e.getMessage());
//            errorResponse.put("timestamp", System.currentTimeMillis());
//
//            return ResponseEntity.badRequest().body(errorResponse);
//        }
//    }

    /**
     * 获取文章点赞总数（从数据库）
     * GET /api/articles/{articleId}/likes/count
     */
    @GetMapping("/{articleId}/likes/count")
    public ResponseEntity<Map<String, Object>> getArticleLikeCount(@PathVariable Long articleId) {
        try {
            Long likeCount = articleLikeService.getArticleLikeCount(articleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("articleId", articleId);
            response.put("likeCount", likeCount);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting like count for article: {}", articleId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("articleId", articleId);
            errorResponse.put("message", "查询点赞数失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 批量获取多篇文章的点赞总数（减少数据库连接）
     * GET /api/articles/likes/batch-count?articleIds=1,2,3,4,5
     */
    @GetMapping("/likes/batch-count")
    public ResponseEntity<Map<String, Object>> getBatchArticleLikeCounts(
            @RequestParam String articleIds) {
        try {
            // 解析文章ID列表
            List<Long> idList = Arrays.stream(articleIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            Map<Long, Long> likeCounts = articleLikeService.getBatchArticleLikeCounts(idList);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("likeCounts", likeCounts);
            response.put("total", likeCounts.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting batch like counts for articles: {}", articleIds, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "批量查询点赞数失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 测试 Kafka 消息发送（调试用）
     * POST /api/articles/{articleId}/test-kafka
     */
    @PostMapping("/{articleId}/test-kafka")
    public ResponseEntity<Map<String, Object>> testKafka(@PathVariable Long articleId) {
        try {
            log.info("Testing Kafka message sending for article: {}", articleId);
            
            // 直接发送测试消息到 Kafka
            Long userId = (long) (Math.random() * 10000 + 1);
            articleLikeService.sendTestKafkaMessage(articleId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("articleId", articleId);
            response.put("userId", userId);
            response.put("message", "测试消息已发送到 Kafka");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error testing Kafka for article: {}", articleId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("articleId", articleId);
            errorResponse.put("message", "Kafka 测试失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

