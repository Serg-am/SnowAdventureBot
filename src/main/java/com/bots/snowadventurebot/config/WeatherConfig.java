package com.bots.snowadventurebot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Data
@Component
@ConfigurationProperties(prefix = "weather")
public class WeatherConfig {
    String apiTemplate;
    String apiKey;
    String userAgent;
}
