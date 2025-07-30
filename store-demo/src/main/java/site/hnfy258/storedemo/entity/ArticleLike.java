package site.hnfy258.storedemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("article_likes")
public class ArticleLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("article_id")
    private Long articleId;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("like_time")
    private Date likeTime;
}
