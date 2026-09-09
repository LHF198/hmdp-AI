package com.hmdp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hmdp.dto.Result;
import com.hmdp.service.IMessageService;
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
    private IMessageService messageService;

    /**
     * 我的笔记收到的评论
     * 返回：评论内容、评论人（昵称/头像）、关联笔记（标题/图片/时间）
     */
    @GetMapping("/comments")
    public Result comments() {
        return messageService.commentsToMe(UserHolder.getUser().getId());
    }

    /**
     * 关注我的人
     * 返回：关注者（昵称/头像）、关注时间
     */
    @GetMapping("/follows")
    public Result follows() {
        return messageService.followers(UserHolder.getUser().getId());
    }
}
