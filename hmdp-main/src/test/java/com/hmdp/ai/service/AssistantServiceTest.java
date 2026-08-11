package com.hmdp.ai.service;

import com.hmdp.ai.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * AssistantService 单元测试：覆盖会话 ID 生成/复用与格式校验（不依赖模型与 Redis）
 */
class AssistantServiceTest {

    private final ChatClient chatClient = mock(ChatClient.class);
    private final ChatMemory chatMemory = mock(ChatMemory.class);
    private final AssistantService service = new AssistantService(chatClient, chatMemory);

    private ChatRequest requestWithId(String conversationId) {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        request.setConversationId(conversationId);
        return request;
    }

    @Test
    void resolveConversationId_未传会话ID时生成32位hex() {
        String id = service.resolveConversationId(requestWithId(null));

        assertEquals(32, id.length());
        assertTrue(id.matches("[0-9a-f]{32}"));
    }

    @Test
    void resolveConversationId_合法会话ID原样返回并去空白() {
        // 前端会话 ID 为毫秒时间戳（13 位纯数字），必须放行
        assertEquals("1754899200000", service.resolveConversationId(requestWithId(" 1754899200000 ")));
    }

    @Test
    void resolveConversationId_含非法字符时拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> service.resolveConversationId(requestWithId("abc; drop table")));
        assertThrows(IllegalArgumentException.class,
                () -> service.resolveConversationId(requestWithId("中文会话")));
    }

    @Test
    void resolveConversationId_长度超限时拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> service.resolveConversationId(requestWithId("a".repeat(65))));
    }

    @Test
    void clearConversation_非法会话ID时拒绝且不删除() {
        assertThrows(IllegalArgumentException.class, () -> service.clearConversation("bad/id!"));

        verify(chatMemory, never()).clear("bad/id!");
    }
}
