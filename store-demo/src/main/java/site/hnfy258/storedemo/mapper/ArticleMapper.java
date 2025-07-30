package site.hnfy258.storedemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import site.hnfy258.storedemo.entity.Article;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}