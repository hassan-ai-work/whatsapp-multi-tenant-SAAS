package com.levosoft.microservice.chat.config;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfiguration {

    @Bean
    public RestClient restClient(ChatProperties chatProperties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(chatProperties.outbound().timeout())
                .build();
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }
}

