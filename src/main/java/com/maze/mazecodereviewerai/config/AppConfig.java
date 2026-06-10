package com.enatbank.codereviewer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Value("${claude.api.key}")
    private String claudeApiKey;

    @Value("${claude.api.base-url}")
    private String claudeBaseUrl;

    @Value("${claude.api.version}")
    private String claudeApiVersion;

    @Value("${nlp.pipeline.colab.base-url}")
    private String colabBaseUrl;

    // ── WebClient for Claude API ──────────────────────────────────────────────
    @Bean("claudeWebClient")
    public WebClient claudeWebClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

        return WebClient.builder()
            .baseUrl(claudeBaseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("x-api-key", claudeApiKey)
            .defaultHeader("anthropic-version", claudeApiVersion)
            .exchangeStrategies(strategies)
            .build();
    }

    // ── WebClient for Colab NLP server ────────────────────────────────────────
    @Bean("colabWebClient")
    public WebClient colabWebClient() {
        return WebClient.builder()
            .baseUrl(colabBaseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    // ── OpenAPI / Swagger ─────────────────────────────────────────────────────
    @Bean
    public OpenAPI openApiConfig() {
        return new OpenAPI()
            .info(new Info()
                .title("AI Code Reviewer API")
                .description("Production-ready code review engine: Spring Boot + Claude LLM + NLP pipeline")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Mazengia Tesfa")
                    .email("dev@enatbank.com")));
    }
}
