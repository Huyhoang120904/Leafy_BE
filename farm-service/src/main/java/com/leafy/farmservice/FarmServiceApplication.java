package com.leafy.farmservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.leafy.farmservice", "com.leafy.common"})
@EnableFeignClients
public class FarmServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarmServiceApplication.class, args);
    }

}
