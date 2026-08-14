package com.hmdp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口允许匿名访问（无需登录）。
 * <p>
 * 鉴权策略为「默认拒绝 + 显式放行」：未标注本注解的 Controller 方法默认需要登录，
 * 由 {@link com.hmdp.utils.LoginInterceptor} 统一拦截校验。
 * </p>
 * <p>
 * 标注在类上表示该类所有接口匿名，标注在方法上表示该方法匿名（方法级优先于类级）。
 * 相比旧版基于 {@code excludePathPatterns} 的路径匹配（{@code /blog/*} 单层通配导致
 * 鉴权隐式依赖 URL 层级），注解驱动让每个接口的鉴权语义显式可见，新增接口默认安全。
 * </p>
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Anonymous {
}
