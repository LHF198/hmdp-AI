package com.hmdp.ai.memory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RedisChatMemory 单元测试：验证 JSON 序列化/反序列化与 Lua 脚本调用参数 （不依赖真实 Redis，使用 Mockito 模拟
 * StringRedisTemplate）
 */
class RedisChatMemoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造带指定键取值行为的 StringRedisTemplate mock（避免嵌套 stubbing）
     */
    private StringRedisTemplate mockTemplate(String key, String value) {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(valueOps.get(key)).thenReturn(value);
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.opsForValue()).thenReturn(valueOps);
        return template;
    }

    private RedisChatMemory newMemory(StringRedisTemplate template) {
        return new RedisChatMemory(template, objectMapper, Duration.ofDays(1));
    }

    @Test
    void get_无历史时返回空列表() {
        StringRedisTemplate template = mockTemplate("ai:chat:memory:s1", null);

        List<Message> messages = newMemory(template).get("s1");

        assertTrue(messages.isEmpty());
    }

    @Test
    void get_合法JSON还原用户与助手消息() throws Exception {
        String json = objectMapper.writeValueAsString(List.of(
                Map.of("type", "USER", "content", "你好"),
                Map.of("type", "ASSISTANT", "content", "嗨！")
        ));
        StringRedisTemplate template = mockTemplate("ai:chat:memory:s1", json);

        List<Message> messages = newMemory(template).get("s1");

        assertEquals(2, messages.size());
        assertInstanceOf(UserMessage.class, messages.get(0));
        assertEquals("你好", messages.get(0).getText());
        assertInstanceOf(AssistantMessage.class, messages.get(1));
        assertEquals("嗨！", messages.get(1).getText());
    }

    @Test
    void get_损坏JSON时返回空列表不抛异常() {
        StringRedisTemplate template = mockTemplate("ai:chat:memory:s1", "{not-json");

        List<Message> messages = newMemory(template).get("s1");

        assertTrue(messages.isEmpty());
    }

    @Test
    void add_过滤工具消息仅持久化对话消息并通过Lua原子追加() throws Exception {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.opsForValue()).thenReturn(mock(ValueOperations.class));
        RedisChatMemory memory = newMemory(template);

        // ToolResponseMessage 构造器在 Spring AI 1.1+ 中为 protected，改用 mock 模拟工具消息（仅验证过滤行为）
        Message toolMessage = mock(Message.class);
        when(toolMessage.getMessageType()).thenReturn(MessageType.TOOL);
        memory.add("s1", List.of(
                new UserMessage("推荐一家美食店"),
                toolMessage,
                new AssistantMessage("好的，为您找到：川味观")
        ));

        verify(template).execute(
                any(DefaultRedisScript.class),
                eq(List.of("ai:chat:memory:s1")),
                eq(objectMapper.writeValueAsString(List.of(
                        Map.of("type", "USER", "content", "推荐一家美食店"),
                        Map.of("type", "ASSISTANT", "content", "好的，为您找到：川味观")
                ))),
                eq("86400")
        );
    }

    @Test
    void add_空消息列表不调用Redis() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.opsForValue()).thenReturn(mock(ValueOperations.class));

        newMemory(template).add("s1", List.of());

        verify(template, org.mockito.Mockito.never())
                .execute(any(DefaultRedisScript.class), any(), any(Object[].class));
    }

    @Test
    void clear_删除对应Redis键() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.opsForValue()).thenReturn(mock(ValueOperations.class));

        newMemory(template).clear("s1");

        verify(template).delete("ai:chat:memory:s1");
    }
}
