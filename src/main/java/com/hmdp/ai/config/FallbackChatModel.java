package com.hmdp.ai.config;

import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

/**
 * 降级 ChatModel：AI_API_KEY 未配置时替代 OpenAI ChatModel， 使 AI
 * 问答接口返回友好提示而非"服务不可用"异常（流式/非流式均可用）。
 */
public class FallbackChatModel implements ChatModel {

    /**
     * 返回给用户的提示文案
     */
    public static final String TIP_TEXT = "AI 助手暂不可用：服务端未配置 AI_API_KEY 环境变量，请联系管理员启用后再试。";

    @Override
    public ChatResponse call(Prompt prompt) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(TIP_TEXT))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(call(prompt));
    }
}
