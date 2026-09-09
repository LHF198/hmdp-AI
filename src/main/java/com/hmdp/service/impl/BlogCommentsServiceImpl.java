package com.hmdp.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.service.IBlogCommentsService;

import jakarta.annotation.Resource;

/**
 * <p>
 * 笔记评论服务实现类：评论分页查询（JOIN 用户表取昵称/头像）、我的评论列表（JOIN 笔记表）
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public Result myComments(Long userId) {
        String sql = """
                SELECT c.id, c.content, c.create_time,
                       b.id AS blog_id, b.title AS blog_title, b.images AS blog_images
                FROM tb_blog_comments c
                JOIN tb_blog b ON b.id = c.blog_id
                WHERE c.user_id = ?
                ORDER BY c.create_time DESC
                LIMIT 50
                """;
        return Result.ok(jdbcTemplate.queryForList(sql, userId));
    }

    @Override
    public Result pageByBlog(Long blogId, Integer current) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_blog_comments WHERE blog_id = ?", Long.class, blogId);
        long totalVal = total == null ? 0 : total;
        if (totalVal == 0) {
            return Result.ok(Map.of("records", List.of(), "total", 0));
        }
        int from = (current - 1) * PAGE_SIZE;
        String sql = """
                SELECT c.id, c.user_id, c.parent_id, c.answer_id, c.content, c.create_time,
                       u.nick_name AS user_nick_name, u.icon AS user_icon
                FROM tb_blog_comments c
                JOIN tb_user u ON u.id = c.user_id
                WHERE c.blog_id = ?
                ORDER BY c.create_time DESC
                LIMIT ?, ?
                """;
        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, blogId, from, PAGE_SIZE);
        return Result.ok(Map.of("records", records, "total", totalVal));
    }
}
