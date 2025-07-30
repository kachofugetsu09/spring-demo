package site.hnfy258.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemViewStats {
    private String itemId;
    private Long viewCount;
    private long windowStart; // 窗口开始时间，方便观察
    private long windowEnd;   // 窗口结束时间
}