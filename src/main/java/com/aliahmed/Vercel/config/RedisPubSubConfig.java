package com.aliahmed.Vercel.config;

import com.aliahmed.Vercel.Services.DeploymentEventListener;
import com.aliahmed.Vercel.Services.DeploymentEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Subscribes the app to the deployment-events channel so status changes published by any
 * instance (including the remote worker) reach this instance's SSE clients.
 */
@Configuration
public class RedisPubSubConfig {

    @Bean
    RedisMessageListenerContainer deploymentEventListenerContainer(
            RedisConnectionFactory connectionFactory, DeploymentEventListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new ChannelTopic(DeploymentEventPublisher.CHANNEL));
        return container;
    }
}
