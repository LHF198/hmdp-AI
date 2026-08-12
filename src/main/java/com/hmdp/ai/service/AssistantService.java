package com.hmdp.ai.service;

import com.hmdp.ai.dto.ChatRequest;
import com.hmdp.ai.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 问答服务：单轮 / 多轮（会话记忆由 MessageChatMemoryAdvisor 自动维护）/ 流式（SSE）三种模式
 */
@Slf4j
@Service
public class AssistantService {

    /**
     * 会话 ID 的 advisor 参数键（Spring AI 1.0.0 中为内联字符串，未暴露公开常量）
     */
    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    /**
     * 会话 ID 规则：8~64 位，仅允许字母、数字、连字符（前端会话 ID 为毫秒时间戳，属合法输入）
     */
    private static final Pattern CONVERSATION_ID_PATTERN = Pattern.compile("[A-Za-z0-9-]{8,64}");

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public AssistantService(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    /**
     * 非流式问答：自动维护会话上下文
     */
    public ChatResponse chat(ChatRequest request) {
        String conversationId = resolveConversationId(request);
        String answer = chatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID_KEY, conversationId))
                .user(request.getMessage())
                .call()
                .content();
        return ChatResponse.of(conversationId, answer);
    }

    /**
     * 流式问答（纯文本流，前端逐字渲染）：结束后自动将完整回答写入会话记忆。 流中异常转为可读错误文本输出（SSE
     * 中途无法更改状态码，避免前端只见连接中断）。
     */
    public Flux<String> chatStream(ChatRequest request) {
        String conversationId = resolveConversationId(request);
        return chatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID_KEY, conversationId))
                .user(request.getMessage())
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.error("AI 流式问答失败，conversationId={}", conversationId, e);
                    return Flux.just("\n\n[出错了] 暂时无法回答，请稍后重试");
                });
    }

    /**
     * 清空指定会话
     */
    public void clearConversation(String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            validateConversationId(conversationId);
            chatMemory.clear(conversationId);
        }
    }

    /**
     * 生成/复用会话 ID（package-private 便于单元测试）
     */
    String resolveConversationId(ChatRequest request) {
        if (StringUtils.hasText(request.getConversationId())) {
            String conversationId = request.getConversationId().trim();
            validateConversationId(conversationId);
            return conversationId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 会话 ID 格式校验：避免异常长度/字符进入 Redis key 拼接与记忆存储
     */
    private void validateConversationId(String conversationId) {
        if (!CONVERSATION_ID_PATTERN.matcher(conversationId).matches()) {
            throw new IllegalArgumentException("会话ID格式不正确，仅支持8~64位字母、数字或连字符");
        }
    }
}
