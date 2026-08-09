package com.fitness.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import com.fitness.server.knowledge.KnowledgeProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KnowledgeProperties.class)
@MapperScan(basePackages = {"com.fitness.server.mapper", "com.fitness.server.knowledge"}, annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class FitnessServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FitnessServerApplication.class, args);
    }
}
