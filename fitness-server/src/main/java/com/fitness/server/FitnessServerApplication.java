package com.fitness.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fitness.server.mapper")
public class FitnessServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FitnessServerApplication.class, args);
    }
}
