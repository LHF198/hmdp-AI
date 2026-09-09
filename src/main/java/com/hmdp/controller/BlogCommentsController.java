package com.hmdp.controller;


import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hmdp.annotation.Anonymous;
import com.hmdp.dto.CommentAddDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.UserHolder;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

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

    @Resource
    private IBlogCommentsService blogCommentsService;

    @Resource
    private IBlogService blogService;

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
        return blogCommentsService.myComments(user.getId());
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
        return blogCommentsService.pageByBlog(blogId, current);
    }

    /**
     * 发布评论（需登录）：一级评论 parentId=0；回复评论时携带 parentId 与 answerId
     * @param blogId 笔记id
     * @param dto 评论体：content（必填）、parentId、answerId（选填）
     */
    @PostMapping("/{blogId}")
    public Result add(@PathVariable("blogId") Long blogId, @Valid @RequestBody CommentAddDTO dto) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        return blogService.addComment(blogId, user.getId(), dto);
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
        return blogService.deleteComment(id, user.getId());
    }
}
