package site.hnfy258.kafkademo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"site.hnfy258"})
@Slf4j
public class KafkaDemoApplication {

	public static void main(String[] args) {
		log.info("=== 启动 Kafka Demo 应用 ===");
		SpringApplication.run(KafkaDemoApplication.class, args);
		log.info("=== Kafka Demo 应用启动完成 ===");
		log.info("访问 http://localhost:8080 查看控制台");
	}

}
