package com.psyavocat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PsyAvocatApplication {

    public static void main(String[] args) {
        SpringApplication.run(PsyAvocatApplication.class, args);
    }

}
