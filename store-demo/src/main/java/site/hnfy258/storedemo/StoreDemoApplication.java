package site.hnfy258.storedemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@MapperScan("site.hnfy258.storedemo.mapper")
public class StoreDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(StoreDemoApplication.class, args);
	}

}
