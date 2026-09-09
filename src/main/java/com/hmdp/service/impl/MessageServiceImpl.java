package com.hmdp.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.hmdp.dto.Result;
import com.hmdp.service.IMessageService;

import jakarta.annotation.Resource;

/**
 * 消息中心服务实现：跨表 JOIN 查询（tb_blog_comments / tb_follow / tb_user / tb_blog）
 */
@Service
public class MessageServiceImpl implements IMessageService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public Result commentsToMe(Long userId) {
        String sql = """
                SELECT c.id, c.user_id, c.content, c.create_time,
                       u.nick_name AS user_nick_name, u.icon AS user_icon,
                       b.id AS blog_id, b.title AS blog_title, b.images AS blog_images
                FROM tb_blog_comments c
                JOIN tb_blog b ON b.id = c.blog_id
                JOIN tb_user u ON u.id = c.user_id
                WHERE b.user_id = ?
                ORDER BY c.create_time DESC
                LIMIT 50
                """;
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, userId);
        return Result.ok(list);
    }

    @Override
    public Result followers(Long userId) {
        String sql = """
                SELECT f.id, f.user_id, f.create_time,
                       u.nick_name AS user_nick_name, u.icon AS user_icon
                FROM tb_follow f
                JOIN tb_user u ON u.id = f.user_id
                WHERE f.follow_user_id = ?
                ORDER BY f.create_time DESC
                LIMIT 50
                """;
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, userId);
        return Result.ok(list);
    }
}
