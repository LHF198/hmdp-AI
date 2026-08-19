package com.hmdp.service.impl;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;

/**
 * BlogServiceImpl 单元测试：覆盖点赞/取消点赞（含 Redis 失败回滚）与笔记详情查询。 通过 Mockito 隔离 Redis /
 * 数据库依赖（spy 替换 lambdaUpdate() 链式调用）。
 */
class BlogServiceImplTest {

    private static final String LIKED_KEY = "blog:liked:10";

    private BlogServiceImpl blogService;
    private StringRedisTemplate stringRedisTemplate;
    private ZSetOperations<String, String> zSetOperations;
    private LambdaUpdateChainWrapper<Blog> updateChain;

    @BeforeEach
    void setUp() {
        blogService = spy(new BlogServiceImpl());
        stringRedisTemplate = mock(StringRedisTemplate.class);
        zSetOperations = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        updateChain = mock(LambdaUpdateChainWrapper.class);
        when(updateChain.setSql(anyString())).thenReturn(updateChain);
        when(updateChain.eq(any(), any())).thenReturn(updateChain);
        when(updateChain.update()).thenReturn(true);
        doReturn(updateChain).when(blogService).lambdaUpdate();
        ReflectionTestUtils.setField(blogService, "stringRedisTemplate", stringRedisTemplate);

        IUserService userService = mock(IUserService.class);
        User userEntity = new User();
        userEntity.setId(1L);
        userEntity.setNickName("测试用户");
        userEntity.setIcon("default.png");
        when(userService.getById(1L)).thenReturn(userEntity);
        ReflectionTestUtils.setField(blogService, "userService", userService);

        UserDTO user = new UserDTO();
        user.setId(1L);
        UserHolder.saveUser(user);
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    // ==================== likeBlog：点赞 / 取消 / Redis 失败回滚 ====================
    @Test
    void likeBlog_未点赞时增加计数并写入Redis() {
        when(zSetOperations.score(LIKED_KEY, "1")).thenReturn(null);

        Result r = blogService.likeBlog(10L);

        assertTrue(r.getSuccess());
        verify(updateChain).setSql("liked=liked+1");
        verify(zSetOperations).add(eq(LIKED_KEY), eq("1"), anyDouble());
    }

    @Test
    void likeBlog_已点赞时减少计数并移除Redis() {
        when(zSetOperations.score(LIKED_KEY, "1")).thenReturn(1.0);

        Result r = blogService.likeBlog(10L);

        assertTrue(r.getSuccess());
        verify(updateChain).setSql("liked = IF(liked > 0, liked - 1, 0)");
        verify(zSetOperations).remove(LIKED_KEY, "1");
    }

    @Test
    void likeBlog_Redis写入失败时回滚DB计数() {
        when(zSetOperations.score(LIKED_KEY, "1")).thenReturn(null);
        doThrow(new RuntimeException("redis down")).when(zSetOperations).add(anyString(), anyString(), anyDouble());

        Result r = blogService.likeBlog(10L);

        assertTrue(r.getSuccess());
        // 先 +1，Redis 异常后回滚 -1，保证 DB/Redis 状态一致
        verify(updateChain, times(2)).update();
        verify(updateChain).setSql("liked = IF(liked > 0, liked - 1, 0)");
    }

    @Test
    void likeBlog_Redis删除失败时回滚DB计数() {
        when(zSetOperations.score(LIKED_KEY, "1")).thenReturn(1.0);
        doThrow(new RuntimeException("redis down")).when(zSetOperations).remove(anyString(), anyString());

        Result r = blogService.likeBlog(10L);

        assertTrue(r.getSuccess());
        verify(updateChain, times(2)).update();
        verify(updateChain).setSql("liked=liked+1");
    }

    // ==================== queryBlogById：笔记查询 ====================
    @Test
    void queryBlogById_笔记不存在时返回失败() {
        doReturn(null).when(blogService).getById(10L);

        Result r = blogService.queryBlogById(10L);

        assertEquals("笔记不存在！", r.getErrorMsg());
    }

    @Test
    void queryBlogById_未登录时返回笔记且不查点赞状态() {
        Blog blog = new Blog();
        blog.setId(10L);
        blog.setUserId(1L);
        doReturn(blog).when(blogService).getById(10L);
        UserHolder.removeUser();

        Result r = blogService.queryBlogById(10L);

        assertTrue(r.getSuccess());
        verify(zSetOperations, times(0)).score(anyString(), anyString());
    }

    // ==================== deleteBlog：权限校验与资源清理 ====================

    @Test
    void deleteBlog_未登录时拒绝() {
        UserHolder.removeUser();

        Result r = blogService.deleteBlog(10L);

        assertEquals("请先登录", r.getErrorMsg());
    }

    @Test
    void deleteBlog_笔记不存在时拒绝() {
        doReturn(null).when(blogService).getById(10L);

        Result r = blogService.deleteBlog(10L);

        assertEquals("笔记不存在！", r.getErrorMsg());
    }

    @Test
    void deleteBlog_非本人笔记时拒绝() {
        Blog blog = new Blog();
        blog.setId(10L);
        blog.setUserId(999L); // 他人笔记
        doReturn(blog).when(blogService).getById(10L);

        Result r = blogService.deleteBlog(10L);

        assertEquals("只能删除自己的笔记！", r.getErrorMsg());
        // 不应触发删除操作
        verify(blogService, never()).removeById(any());
    }

    @Test
    void deleteBlog_本人笔记时删除成功() {
        Blog blog = new Blog();
        blog.setId(10L);
        blog.setUserId(1L); // 本人
        blog.setImages("");
        doReturn(blog).when(blogService).getById(10L);
        doReturn(true).when(blogService).removeById(10L);

        // Mock followService（无粉丝）
        IFollowService followService = mock(IFollowService.class);
        LambdaQueryChainWrapper<Follow> followQuery = mock(LambdaQueryChainWrapper.class);
        when(followService.lambdaQuery()).thenReturn(followQuery);
        when(followQuery.eq(any(), any())).thenReturn(followQuery);
        when(followQuery.list()).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(blogService, "followService", followService);

        // Mock blogCommentsService
        IBlogCommentsService blogCommentsService = mock(IBlogCommentsService.class);
        ReflectionTestUtils.setField(blogService, "blogCommentsService", blogCommentsService);

        Result r = blogService.deleteBlog(10L);

        assertTrue(r.getSuccess());
        verify(blogService).removeById(10L);
        // 事务提交后清理 Redis（无事务上下文时同步执行）
        verify(stringRedisTemplate).delete("blog:liked:10");
    }
}
