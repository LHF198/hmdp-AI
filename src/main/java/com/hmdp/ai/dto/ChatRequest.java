package com.hmdp.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天请求
 */
@Data
public class ChatRequest {

    /**
     * 用户输入内容（必填）
     */
    @NotBlank(message = "提问内容不能为空")
    @Size(max = 2000, message = "提问内容过长（最多2000字）")
    private String message;

    /**
     * 会话 ID（多轮对话时传入以保持上下文，为空则创建新会话）
     */
    private String conversationId;
}
