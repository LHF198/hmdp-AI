package com.hmdp.ai.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 基于 Redis 的会话记忆（对齐参考项目实现）： 以 JSON 形式按 conversationId 存储消息列表，TTL 1 天，重启不丢失。
 *
 * <p>
 * 实现 {@link ChatMemory} 接口，配合 Spring AI 的 MessageChatMemoryAdvisor 使用。
 */
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "ai:chat:memory:";

    private static final TypeReference<List<Map<String, String>>> LIST_MAP_TYPE
            = new TypeReference<List<Map<String, String>>>() {
    };

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    /**
     * 会话记忆原子追加脚本：服务端一次性完成 读旧消息 -> 合并新消息 -> 截断最近 20 条 -> 写入并刷新 TTL。 避免并发请求下 Java
     * 侧 get-merge-set 竞态互相覆盖丢失消息。
     */
    private static final DefaultRedisScript<Long> APPEND_SCRIPT = new DefaultRedisScript<>(
            "local old = redis.call('get', KEYS[1])\n"
            + "local newArr = cjson.decode(ARGV[1])\n"
            + "local merged = {}\n"
            + "if old then\n"
            + "    local ok, oldArr = pcall(cjson.decode, old)\n"
            + "    if ok and type(oldArr) == 'table' then\n"
            + "        for i, v in ipairs(oldArr) do table.insert(merged, v) end\n"
            + "    end\n"
            + "end\n"
            + "for i, v in ipairs(newArr) do table.insert(merged, v) end\n"
            + "local max = 20\n"
            + "if #merged > max then\n"
            + "    local trimmed = {}\n"
            + "    for i = #merged - max + 1, #merged do table.insert(trimmed, merged[i]) end\n"
            + "    merged = trimmed\n"
            + "end\n"
            + "redis.call('set', KEYS[1], cjson.encode(merged), 'EX', ARGV[2])\n"
            + "return #merged",
            Long.class
    );

    public RedisChatMemory(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper, Duration ttl) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public List<Message> get(String conversationId) {
        String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + conversationId);
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, String>> records = objectMapper.readValue(json, LIST_MAP_TYPE);
            List<Message> messages = new ArrayList<>(records.size());
            for (Map<String, String> record : records) {
                MessageType type = MessageType.valueOf(record.get("type"));
                String content = record.get("content");
                switch (type) {
                    case USER ->
                        messages.add(new UserMessage(content));
                    case ASSISTANT ->
                        messages.add(new AssistantMessage(content));
                    case SYSTEM ->
                        messages.add(new SystemMessage(content));
                    default -> {
                        // 工具调用等其他类型暂不持久化，避免影响上下文
                    }
                }
            }
            return messages;
        } catch (Exception e) {
            // 解析失败时视为无历史，避免影响对话
            return List.of();
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        // 仅持久化 USER/ASSISTANT/SYSTEM 类型，工具调用等不影响上下文
        List<Map<String, String>> records = messages.stream()
                .filter(m -> m.getMessageType() == MessageType.USER
                || m.getMessageType() == MessageType.ASSISTANT
                || m.getMessageType() == MessageType.SYSTEM)
                .map(m -> Map.of("type", m.getMessageType().name(), "content", m.getText()))
                .toList();
        try {
            // 合并、截断最近 20 条、写入并刷新 TTL 全部在 Lua 中原子完成
            stringRedisTemplate.execute(
                    APPEND_SCRIPT,
                    Collections.singletonList(KEY_PREFIX + conversationId),
                    objectMapper.writeValueAsString(records),
                    String.valueOf(ttl.toSeconds())
            );
        } catch (Exception e) {
            throw new IllegalStateException("保存会话记忆到 Redis 失败", e);
        }
    }

    @Override
    public void clear(String conversationId) {
        stringRedisTemplate.delete(KEY_PREFIX + conversationId);
    }
}
