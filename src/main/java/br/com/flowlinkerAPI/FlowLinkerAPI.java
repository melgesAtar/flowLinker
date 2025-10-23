package br.com.flowlinkerAPI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import java.util.TimeZone;

@SpringBootApplication
@EnableRabbit
public class FlowLinkerAPI {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        SpringApplication.run(FlowLinkerAPI.class, args);
    }
}