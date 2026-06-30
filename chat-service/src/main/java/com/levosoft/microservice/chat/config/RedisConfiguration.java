package com.levosoft.microservice.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.PatternTopic;

import com.levosoft.microservice.chat.listener.SessionExpiryListener;

@Configuration
public class RedisConfiguration {


    @Bean
    @ConditionalOnProperty(prefix = "chat.redis", name = "keyspace-listener-enabled", havingValue = "true")
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            SessionExpiryListener sessionExpiryListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(sessionExpiryListener, new PatternTopic("__keyevent@0__:expired"));
        return container;
    }
}

