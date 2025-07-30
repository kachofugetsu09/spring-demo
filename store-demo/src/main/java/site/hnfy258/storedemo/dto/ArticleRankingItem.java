package site.hnfy258.storedemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleRankingItem {
    private Long articleId;
    private Long likeCount;
    private Integer rank;
    private String timeWindow;

}