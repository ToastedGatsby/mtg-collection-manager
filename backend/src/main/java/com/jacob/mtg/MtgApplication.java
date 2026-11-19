package com.jacob.mtg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan // picks up AuthProperties, LegalProperties (type-safe config records)
@EnableScheduling // token cleanup now; nightly Scryfall import + demo account reset later
public class MtgApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtgApplication.class, args);
    }
}
