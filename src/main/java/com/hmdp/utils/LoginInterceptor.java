package com.hmdp.utils;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.hmdp.annotation.Anonymous;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 登录拦截器：基于 {@link Anonymous} 注解的「默认拒绝 + 显式放行」鉴权。
 * <p>
 * 处理规则：
 * <ul>
 * <li>非 {@link HandlerMethod}（静态资源、错误页、Actuator 端点等）直接放行；</li>
 * <li>方法或所在类标注了 {@link Anonymous} 则匿名放行；</li>
 * <li>其余接口要求登录，ThreadLocal 中无用户则返回 401。</li>
 * </ul>
 * 相比旧版基于 {@code excludePathPatterns} 的路径匹配，注解驱动让每个接口的鉴权语义
 * 显式可见，新增接口默认安全（需登录），消除「{@code /blog/*} 只匹配单层导致鉴权隐式
 * 依赖 URL 层级」的脆弱设计。
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.静态资源、错误页、Actuator 等非 Controller 方法直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        // 2.方法或类标注了 @Anonymous 则匿名放行（方法级优先于类级）
        if (handlerMethod.hasMethodAnnotation(Anonymous.class)
                || handlerMethod.getBeanType().isAnnotationPresent(Anonymous.class)) {
            return true;
        }
        // 3.其余接口要求登录：ThreadLocal 中无用户则返回 401
        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
