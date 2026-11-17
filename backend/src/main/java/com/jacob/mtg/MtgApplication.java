package com.jacob.mtg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // nightly Scryfall import + demo account reset will use @Scheduled
public class MtgApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtgApplication.class, args);
    }
}
