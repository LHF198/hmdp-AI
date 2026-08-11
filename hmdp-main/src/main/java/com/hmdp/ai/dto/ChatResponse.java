package com.hmdp.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** 会话 ID，客户端需保存并在后续请求中带回 */
    private String conversationId;

    /** 助手回答 */
    private String answer;

    /** 响应时间戳 */
    private long timestamp;

    public static ChatResponse of(String conversationId, String answer) {
        return new ChatResponse(conversationId, answer, System.currentTimeMillis());
    }
}
