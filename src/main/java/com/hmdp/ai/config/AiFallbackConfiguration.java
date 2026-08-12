package com.hmdp.ai.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 密钥缺失时的降级装配：由 {@link AiFallbackEnvironmentPostProcessor} 置位开关后生效， 提供降级
 * ChatModel / EmbeddingModel，保证应用正常启动、AI 接口返回配置提示。 已配置 AI_API_KEY 时本配置不生效（正常走
 * OpenAI 自动配置）。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = AiFallbackEnvironmentPostProcessor.FALLBACK_ENABLED, havingValue = "true")
public class AiFallbackConfiguration {

    @Bean
    public ChatModel fallbackChatModel() {
        return new FallbackChatModel();
    }

    @Bean
    public EmbeddingModel fallbackEmbeddingModel() {
        return new FallbackEmbeddingModel();
    }
}
