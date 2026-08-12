package com.hmdp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hmdp.dto.Result;
import com.hmdp.utils.UserHolder;

import jakarta.annotation.Resource;

/**
 * 消息中心：我的笔记收到的评论、关注我的人
 * 数据直接来自业务表（tb_blog_comments / tb_follow），无需额外建通知表
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 我的笔记收到的评论
     * 返回：评论内容、评论人（昵称/头像）、关联笔记（标题/图片/时间）
     */
    @GetMapping("/comments")
    public Result comments() {
        Long userId = UserHolder.getUser().getId();
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

    /**
     * 关注我的人
     * 返回：关注者（昵称/头像）、关注时间
     */
    @GetMapping("/follows")
    public Result follows() {
        Long userId = UserHolder.getUser().getId();
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
