package com.hmdp.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;

/**
 * <p>
 * 笔记评论服务接口：评论分页查询（含评论人昵称/头像）、我的评论列表
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogCommentsService extends IService<BlogComments> {

    /** 评论列表每页条数 */
    int PAGE_SIZE = 10;

    /**
     * 查询用户发出的评论（个人主页“评价”Tab，无需分页，最近 50 条）
     * 返回：评论内容、时间、关联笔记（标题/首图）
     */
    Result myComments(Long userId);

    /**
     * 分页查询某笔记的评论，按时间倒序，附带评论人昵称/头像
     *
     * @param blogId  笔记id
     * @param current 页码
     * @return records（评论列表）+ total（总数）
     */
    Result pageByBlog(Long blogId, Integer current);
}
