package site.hnfy258.storedemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.hnfy258.storedemo.entity.Article;
import site.hnfy258.storedemo.entity.ArticleLike;
import site.hnfy258.storedemo.mapper.ArticleMapper;
import site.hnfy258.storedemo.service.ArticleLikeService;
import site.hnfy258.storedemo.service.ArticleService;
import site.hnfy258.storedemo.service.KafkaConsumerService;

import java.util.Date;
import java.util.Random;

@Service
public class ArticleServiceImpl  extends ServiceImpl<ArticleMapper, Article> implements ArticleService {


}
