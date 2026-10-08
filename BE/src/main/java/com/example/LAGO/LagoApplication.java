package com.example.LAGO;

//import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableCaching	// 스프링 캐시 활성화
@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = "com.example.LAGO")
@EntityScan(basePackages = "com.example.LAGO.domain")
@EnableJpaRepositories(basePackages = "com.example.LAGO.repository")
public class LagoApplication {

	public static void main(String[] args) {

		SpringApplication.run(LagoApplication.class, args);
	}

}
