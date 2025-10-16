package br.com.flowlinkerAPI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@SpringBootApplication
@EnableRabbit
public class FlowLinkerAPI {
    public static void main(String[] args) {
        SpringApplication.run(FlowLinkerAPI.class, args);
    }
}