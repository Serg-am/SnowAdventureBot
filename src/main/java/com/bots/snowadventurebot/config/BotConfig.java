package com.bots.snowadventurebot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Configuration
@EnableScheduling
@Data
@Component
@ConfigurationProperties(prefix = "bot")
public class BotConfig {
    String name;
    String token;
    Long owner;
}
