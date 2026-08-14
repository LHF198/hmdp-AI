package com.hmdp.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import com.hmdp.annotation.Anonymous;
import com.hmdp.dto.UserDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * LoginInterceptor 单元测试：覆盖注解驱动鉴权的全部决策分支。
 * <ul>
 * <li>非 HandlerMethod（静态资源/Actuator）放行；</li>
 * <li>方法级 / 类级 @Anonymous 匿名放行；</li>
 * <li>未标注且未登录返回 401 并拦截；</li>
 * <li>未标注但已登录（ThreadLocal 有用户）放行。</li>
 * </ul>
 * 纯 Mockito 单测：mock request/response，不启动 Spring 上下文。
 * 注意：HandlerMethod 需用实例构造器 {@code new HandlerMethod(bean, method)}，
 * 传入 Class 对象会被当作 bean 实例，导致 getBeanType() 返回 Class.class。
 */
class LoginInterceptorTest {

    /** 方法级匿名：仅 anonymousMethod 允许匿名访问 */
    static class MixedController {
        @Anonymous
        public void anonymousMethod() {
        }

        public void protectedMethod() {
        }
    }

    /** 类级匿名：全部方法允许匿名访问 */
    @Anonymous
    static class AnonymousController {
        public void anyMethod() {
        }
    }

    private LoginInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new LoginInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        UserHolder.removeUser();
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    private HandlerMethod handlerMethod(Object bean, String methodName) throws NoSuchMethodException {
        return new HandlerMethod(bean, bean.getClass().getMethod(methodName));
    }

    // ==================== 非 HandlerMethod ====================

    @Test
    void nonHandlerMethod_shouldPassThrough() throws Exception {
        // 静态资源、错误页、Actuator 等 handler 不是 HandlerMethod，一律放行
        boolean pass = interceptor.preHandle(request, response, new Object());

        assertTrue(pass);
        verify(response, never()).setStatus(401);
    }

    // ==================== @Anonymous 匿名放行 ====================

    @Test
    void methodLevelAnonymous_shouldPassWithoutLogin() throws Exception {
        boolean pass = interceptor.preHandle(request, response,
                handlerMethod(new MixedController(), "anonymousMethod"));

        assertTrue(pass);
        verify(response, never()).setStatus(401);
    }

    @Test
    void classLevelAnonymous_shouldPassWithoutLogin() throws Exception {
        boolean pass = interceptor.preHandle(request, response,
                handlerMethod(new AnonymousController(), "anyMethod"));

        assertTrue(pass);
        verify(response, never()).setStatus(401);
    }

    // ==================== 需登录接口 ====================

    @Test
    void noAnnotation_withoutUser_shouldRejectWith401() throws Exception {
        // 未标注 @Anonymous 且 ThreadLocal 无用户 → 拦截并返回 401
        boolean pass = interceptor.preHandle(request, response,
                handlerMethod(new MixedController(), "protectedMethod"));

        assertFalse(pass);
        verify(response).setStatus(401);
    }

    @Test
    void noAnnotation_withUser_shouldPass() throws Exception {
        UserDTO user = new UserDTO();
        user.setId(1L);
        UserHolder.saveUser(user);

        boolean pass = interceptor.preHandle(request, response,
                handlerMethod(new MixedController(), "protectedMethod"));

        assertTrue(pass);
        verify(response, never()).setStatus(401);
    }
}
