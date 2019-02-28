package com.ailu.firmoffer;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@EnableScheduling
@SpringBootApplication
@MapperScan(basePackages = "com.ailu.firmoffer.dao.mapper")
public class FirmOfferApplication {

    public static ApplicationContext applicationContext;

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        applicationContext = SpringApplication.run(FirmOfferApplication.class, args);
    }

    @Bean
    public ScheduledExecutorService taskScheduler() {
        return Executors.newScheduledThreadPool(10);
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("firmOffer");
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(20);
        return taskExecutor;
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Request.Builder getRequestBuilder() {
        return new Request.Builder();
    }

}
