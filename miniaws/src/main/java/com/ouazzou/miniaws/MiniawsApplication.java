package com.ouazzou.miniaws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // ⬅️ Active les tâches répétitives en arrière-plan
@EnableAsync
public class MiniawsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniawsApplication.class, args);
    }

}
