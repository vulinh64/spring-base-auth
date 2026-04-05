package com.vulinh;

import com.vulinh.aspect.ExecutionTimeAspect;
import com.vulinh.configuration.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableCaching
@EnableAsync
@EnableJpaAuditing
@Import(ExecutionTimeAspect.class)
public class SpringBaseAuthApplication {

  static void main(String[] args) {
    SpringApplication.run(SpringBaseAuthApplication.class, args);
  }
}
