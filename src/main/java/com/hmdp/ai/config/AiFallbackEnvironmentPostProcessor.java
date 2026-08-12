package com.hmdp.ai.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * AI 密钥缺失时的降级处理（环境准备阶段执行）：
 * <p>
 * Spring AI 1.0.0 的 OpenAI 自动配置在 api-key 为空时强校验并中止启动 （"OpenAI API key must be
 * set"）。当 AI_API_KEY 未注入时，本处理器：
 * <ul>
 * <li>通过 spring.autoconfigure.exclude 排除全部 OpenAI 自动配置 （chat / embedding /
 * image / audio / moderation），保证应用可启动；</li>
 * <li>置 ai.fallback.enabled=true，由 {@link AiFallbackConfiguration} 提供降级
 * ChatModel / EmbeddingModel，AI 接口返回"未配置"提示而非报错。</li>
 * </ul>
 * 已配置 AI_API_KEY 时本处理器不产生任何影响。
 * <p>
 * 通过 META-INF/spring.factories 注册（EnvironmentPostProcessor 唯一支持的方式）。
 */
public class AiFallbackEnvironmentPostProcessor implements EnvironmentPostProcessor {

    /**
     * 降级模式开关属性名（被 {@link AiFallbackConfiguration} 的条件注解读取）
     */
    public static final String FALLBACK_ENABLED = "ai.fallback.enabled";

    /**
     * 无密钥时排除的 OpenAI 自动配置（Spring AI 1.0 对空 key 强校验，逐个覆盖 spring.ai.model.* 易遗漏）
     */
    private static final List<String> EXCLUDED_OPENAI_AUTO_CONFIGS = List.of(
            "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // spring.ai.openai.api-key 由 ${AI_API_KEY:} 占位符解析而来：未注入时为空白串
        String apiKey = environment.getProperty("spring.ai.openai.api-key");
        if (StringUtils.hasText(apiKey)) {
            return; // 已配置密钥：维持正常 OpenAI 自动配置
        }

        Map<String, Object> overrides = new HashMap<>();
        overrides.put(FALLBACK_ENABLED, "true");
        // 与外部已有的 exclude 合并（如测试类通过 @SpringBootTest(properties=...) 补充的排除项）
        List<String> excludes = new ArrayList<>(EXCLUDED_OPENAI_AUTO_CONFIGS);
        String existing = environment.getProperty("spring.autoconfigure.exclude");
        if (existing != null && !existing.isBlank()) {
            for (String item : existing.split(",")) {
                if (!item.isBlank()) {
                    excludes.add(item.trim());
                }
            }
        }
        overrides.put("spring.autoconfigure.exclude", String.join(",", excludes));
        environment.getPropertySources().addFirst(new MapPropertySource("ai-fallback-overrides", overrides));
    }
}
