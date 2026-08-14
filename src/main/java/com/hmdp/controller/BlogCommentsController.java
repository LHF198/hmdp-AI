package com.hmdp.controller;


import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.annotation.Anonymous;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.BlogComments;
import com.hmdp.enums.CommentStatusEnum;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.UserHolder;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;

/**
 * <p>
 * 笔记评论：分页查询（含评论人昵称/头像）、发布评论与回复、删除本人评论
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {

    /** 评论列表每页条数 */
    private static final int PAGE_SIZE = 10;

    @Resource
    private IBlogCommentsService blogCommentsService;

    @Resource
    private IBlogService blogService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询当前登录用户发出的评论（个人主页“评价”Tab，无需分页，最近 50 条）
     * 返回：评论内容、时间、关联笔记（标题/首图）
     */
    @GetMapping("/of/me")
    public Result myComments() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        String sql = """
                SELECT c.id, c.content, c.create_time,
                       b.id AS blog_id, b.title AS blog_title, b.images AS blog_images
                FROM tb_blog_comments c
                JOIN tb_blog b ON b.id = c.blog_id
                WHERE c.user_id = ?
                ORDER BY c.create_time DESC
                LIMIT 50
                """;
        return Result.ok(jdbcTemplate.queryForList(sql, user.getId()));
    }

    /**
     * 分页查询某笔记的评论（无需登录），按时间倒序，附带评论人昵称/头像
     * @param blogId 笔记id
     * @param current 页码
     * @return records（评论列表）+ total（总数）
     */
    @Anonymous
    @GetMapping("/{blogId}")
    public Result list(@PathVariable("blogId") Long blogId,
                       @RequestParam(value = "current", defaultValue = "1") Integer current) {
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

    /**
     * 发布评论（需登录）：一级评论 parentId=0；回复评论时携带 parentId 与 answerId
     * @param blogId 笔记id
     * @param body 评论体：content（必填）、parentId、answerId（选填）
     */
    @PostMapping("/{blogId}")
    public Result add(@PathVariable("blogId") Long blogId, @RequestBody Map<String, Object> body) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        String content = body.get("content") == null ? "" : body.get("content").toString().trim();
        if (StrUtil.isBlank(content)) {
            return Result.fail("评论内容不能为空");
        }
        if (content.length() > 255) {
            return Result.fail("评论内容过长（最多255字）");
        }
        // 校验笔记存在
        if (blogService.getById(blogId) == null) {
            return Result.fail("笔记不存在");
        }
        BlogComments comment = new BlogComments();
        comment.setBlogId(blogId);
        comment.setUserId(user.getId());
        comment.setContent(content);
        comment.setParentId(body.get("parentId") == null ? 0L : Long.valueOf(body.get("parentId").toString()));
        comment.setAnswerId(body.get("answerId") == null ? 0L : Long.valueOf(body.get("answerId").toString()));
        comment.setLiked(0);
        comment.setStatus(CommentStatusEnum.NORMAL.getCode());
        boolean isSuccess = blogCommentsService.save(comment);
        if (!isSuccess) {
            return Result.fail("评论失败");
        }
        // 同步笔记评论数（与点赞计数同样的 DB 计数模式）
        blogService.incrCommentCount(blogId);
        return Result.ok(comment.getId());
    }

    /**
     * 删除评论（需登录，仅评论作者本人可删）
     * @param id 评论id
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable("id") Long id) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        BlogComments comment = blogCommentsService.getById(id);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        if (!comment.getUserId().equals(user.getId())) {
            return Result.fail("只能删除自己的评论");
        }
        // 统计将删除的条数（评论本身 + 其下回复），用于同步笔记评论数
        long replyCount = blogCommentsService.count(new LambdaQueryWrapper<BlogComments>()
                .eq(BlogComments::getParentId, id));
        // 删除评论本身及其下的所有回复
        blogCommentsService.remove(new LambdaQueryWrapper<BlogComments>()
                .eq(BlogComments::getId, id)
                .or()
                .eq(BlogComments::getParentId, id));
        // 同步笔记评论数（GREATEST 保底防止负数）
        long removed = 1 + replyCount;
        blogService.decrCommentCount(comment.getBlogId(), removed);
        return Result.ok();
    }
}
