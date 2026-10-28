package io.github.mmlegend.reusehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ReuseHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReuseHubApplication.class, args);
    }
}
