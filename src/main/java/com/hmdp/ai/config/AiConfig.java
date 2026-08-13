package com.hmdp.ai.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.hmdp.ai.memory.RedisChatMemory;
import com.hmdp.ai.tool.ShopQueryTool;
import com.hmdp.ai.web.AiRateLimitInterceptor;

/**
 * Spring AI 相关配置：
 * <ul>
 * <li>构建带系统提示词、业务工具、会话记忆（Redis/内存可切换）、RAG 知识库的 ChatClient</li>
 * <li>开放跨域（方便前端页面直接调用）</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    private final AiRateLimitInterceptor aiRateLimitInterceptor;

    /**
     * CORS 预检请求缓存时长（秒），可通过 app.cors.max-age 覆盖
     */
    @Value("${app.cors.max-age:3600}")
    private long corsMaxAge;

    public AiConfig(AiRateLimitInterceptor aiRateLimitInterceptor) {
        this.aiRateLimitInterceptor = aiRateLimitInterceptor;
    }

    /**
     * 会话记忆实现：redis（默认，持久化到 Redis，TTL 可配置）| memory（进程内存）
     */
    @Bean
    public ChatMemory chatMemory(StringRedisTemplate stringRedisTemplate,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            @Value("${app.ai.memory.type:redis}") String memoryType,
            @Value("${app.ai.memory.ttl-days:1}") long ttlDays) {
        if ("memory".equalsIgnoreCase(memoryType)) {
            return MessageWindowChatMemory.builder()
                    .chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .build();
        }
        // 配置值非法（<=0）时回退为 1 天
        Duration ttl = ttlDays > 0 ? Duration.ofDays(ttlDays) : Duration.ofDays(1);
        return new RedisChatMemory(stringRedisTemplate, objectMapper, ttl);
    }

    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
            @Value("classpath:prompts/system-prompt.st") Resource systemPrompt,
            MessageChatMemoryAdvisor messageChatMemoryAdvisor,
            QuestionAnswerAdvisor questionAnswerAdvisor,
            ShopQueryTool shopQueryTool) {
        String promptText;
        try {
            promptText = systemPrompt.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("无法读取系统提示词文件 prompts/system-prompt.st", e);
        }
        return builder
                .defaultSystem(promptText)
                // 会话记忆：自动维护多轮上下文（conversationId 维度）
                .defaultAdvisors(messageChatMemoryAdvisor)
                // RAG：自动检索知识库补充回答
                .defaultAdvisors(questionAnswerAdvisor)
                // 业务工具：模型在需要时自动调用查询店铺 / 优惠券
                .defaultTools(shopQueryTool)
                .build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/ai/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(corsMaxAge);
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                // AI 接口按 IP 限流（Redis 固定窗口），防止模型调用被刷
                registry.addInterceptor(aiRateLimitInterceptor)
                        .addPathPatterns("/api/ai/chat/**");
            }
        };
    }
}
