package com.hmdp.ai.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 无 AI_API_KEY 降级场景验证：应用可正常启动（Spring AI 1.0 对空 key 强校验， 由
 * AiFallbackEnvironmentPostProcessor 禁用 OpenAI 自动配置）， AI 问答返回配置提示而非异常。
 * <p>
 * 已注入 AI_API_KEY 的环境（如重启终端后运行 mvn test）自动跳过本测试。
 */
@SpringBootTest
class AiFallbackContextTest {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ChatClient chatClient;

    @Test
    void chatClientReturnsFallbackTipWithoutApiKey() {
        Assumptions.assumeTrue(chatModel instanceof FallbackChatModel,
                "测试环境已注入 AI_API_KEY，跳过降级场景验证");

        String content = chatClient.prompt().user("你好").call().content();
        assertEquals(FallbackChatModel.TIP_TEXT, content);
    }
}
