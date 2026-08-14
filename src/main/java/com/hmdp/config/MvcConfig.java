package com.hmdp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;

import jakarta.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // token 刷新拦截器（order=0 先执行）：解析请求头 token 到 ThreadLocal，供登录拦截器判断
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
        // 登录拦截器（order=1 后执行）：基于 @Anonymous 注解的「默认拒绝 + 显式放行」鉴权，
        // 不再使用 excludePathPatterns 路径列表，鉴权语义由各 Controller 方法上的注解显式表达
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")
                .order(1);
    }
}
