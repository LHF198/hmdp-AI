package com.hmdp.ai.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hmdp.ai.dto.ChatRequest;
import com.hmdp.ai.service.AssistantService;
import com.hmdp.dto.Result;
import com.hmdp.annotation.Anonymous;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import reactor.core.publisher.Flux;

import org.springframework.validation.annotation.Validated;

/**
 * 智能问答接口
 *
 * <pre>
 *  GET  /api/ai/chat/stream     流式问答（text/html 纯文本流，前端 fetch + ReadableStream 逐字渲染）
 *  GET  /api/ai/health          健康检查
 * </pre>
 */
@Anonymous
@Validated
@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final AssistantService assistantService;

    public ChatController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * 流式问答（纯文本流）：前端逐字渲染，conversationId 由前端生成并带回以维持会话记忆
     */
    @GetMapping(value = "/chat/stream", produces = "text/html;charset=utf-8")
    public Flux<String> chatStreamGet(
            @RequestParam("message") @NotBlank(message = "提问内容不能为空")
            @Size(max = 2000, message = "提问内容过长（最多2000字）") String message,
            @RequestParam(value = "conversationId", required = false)
            @Size(max = 64, message = "会话ID过长") String conversationId) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setConversationId(conversationId);
        return assistantService.chatStream(request);
    }

    /**
     * 健康检查
     */
    @Anonymous
    @GetMapping("/health")
    public Result health() {
        return Result.ok(Map.of(
                "status", "UP",
                "service", "hmdp-ai-assistant",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
