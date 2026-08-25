package com.ET.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.openai")
public class OpenAIProperties {
    private String apiKey;
    private String model = "gpt-4o-mini";
    private int maxTokens = 1000;
    private String url = "https://api.openai.com/v1/chat/completions";
}
