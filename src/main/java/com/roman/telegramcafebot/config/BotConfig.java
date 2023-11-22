package com.roman.telegramcafebot.config;

import lombok.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.beans.factory.annotation.Value;


@Configuration
@Data
@PropertySource("application.properties")
public class BotConfig {
    @Value("${bot.name}")
    String botName;
    @Value("${bot.token}")
    String token;
}
