package com.hmdp.service;

import com.hmdp.dto.Result;

/**
 * 消息中心服务：我的笔记收到的评论、关注我的人
 * 数据直接来自业务表（tb_blog_comments / tb_follow），无需额外建通知表
 */
public interface IMessageService {

    /**
     * 我的笔记收到的评论（最近 50 条）
     * 返回：评论内容、评论人（昵称/头像）、关联笔记（标题/图片/时间）
     */
    Result commentsToMe(Long userId);

    /**
     * 关注我的人（最近 50 条）
     * 返回：关注者（昵称/头像）、关注时间
     */
    Result followers(Long userId);
}
