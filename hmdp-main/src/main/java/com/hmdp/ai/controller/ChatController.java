package com.hmdp.ai.controller;

import com.hmdp.ai.dto.ChatRequest;
import com.hmdp.ai.dto.ChatResponse;
import com.hmdp.ai.service.AssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 智能问答接口
 *
 * <pre>
 *  POST /api/ai/chat            非流式问答（JSON）
 *  POST /api/ai/chat/stream     流式问答（SSE，适合 fetch + ReadableStream）
 *  GET  /api/ai/chat/stream     流式问答（text/html 纯文本流，兼容参考项目前端逐字渲染）
 *  DELETE /api/ai/conversation/{id}  清空会话记忆
 *  GET  /api/ai/health          健康检查
 * </pre>
 */
@Validated
@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final AssistantService assistantService;

    public ChatController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * 非流式问答
     */
    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return assistantService.chat(request);
    }

    /**
     * 流式问答（SSE）：用于前端 fetch + ReadableStream 消费
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        return assistantService.chatStream(request);
    }

    /**
     * 流式问答（纯文本流）：与参考项目 GET /chat 语义一致，
     * 复用参考项目前端 ai-assistant.js 的逐字渲染逻辑
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
     * 清空会话记忆，开启全新对话
     */
    @DeleteMapping("/conversation/{conversationId}")
    public Map<String, Object> clearConversation(@PathVariable @Size(max = 64, message = "会话ID过长") String conversationId) {
        assistantService.clearConversation(conversationId);
        return Map.of("success", true, "conversationId", conversationId);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "hmdp-ai-assistant",
                "timestamp", System.currentTimeMillis()
        );
    }
}
