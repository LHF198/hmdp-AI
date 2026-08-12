package com.hmdp.ai.web;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 接口限流拦截器：按客户端 IP 做 Redis 固定窗口限流（默认每分钟 30 次）， 防止模型按量计费的接口被脚本刷调用。
 *
 * <p>
 * 注意事项：
 * <ul>
 * <li>nginx 反代场景下真实 IP 在 X-Forwarded-For / X-Real-IP 头中（header 可伪造，
 * 单机/内网部署可接受；如需严格防刷应配合网关层做校验）</li>
 * <li>Redis 异常时自动放行，限流组件不阻塞主功能</li>
 * </ul>
 */
@Slf4j
@Component
public class AiRateLimitInterceptor implements HandlerInterceptor {

    private static final String RATE_KEY_PREFIX = "ai:rate:";

    /**
     * 固定窗口格式：分钟级粒度
     */
    private static final DateTimeFormatter WINDOW_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.ai.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.rate-limit.per-minute:30}")
    private int perMinute;

    public AiRateLimitInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!enabled) {
            return true;
        }
        try {
            String key = RATE_KEY_PREFIX + WINDOW_FORMAT.format(LocalDateTime.now()) + ":" + resolveClientIp(request);
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                // 窗口内第一次访问时设置过期时间
                stringRedisTemplate.expire(key, WINDOW);
            }
            if (count != null && count > perMinute) {
                log.warn("AI 接口限流触发：key={}, count={}, limit={}", key, count, perMinute);
                reject(response);
                return false;
            }
            return true;
        } catch (Exception e) {
            // Redis 不可用时放行，避免限流组件导致 AI 功能不可用
            log.warn("AI 限流检查失败，本次请求放行：{}", e.getMessage());
            return true;
        }
    }

    /**
     * 解析客户端 IP：优先取代理透传头，其次取直连地址
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            String first = (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            if (StringUtils.hasText(first)) {
                return first;
            }
        }
        String real = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(real)) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 返回 429 + 与主项目 Result 结构一致的 JSON
     */
    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"errorMsg\":\"请求过于频繁，请稍后再试\"}");
    }
}
