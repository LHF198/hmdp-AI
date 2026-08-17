package com.hmdp.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;

/**
 * <p>
 * 笔记服务接口：发布笔记、分页查询热门笔记（按点赞数排序）
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    /**
     * 分页查询当前登录用户的笔记
     */
    Result queryMyBlog(Integer current);

    /**
     * 分页查询指定用户的笔记
     */
    Result queryBlogByUserId(Long id, Integer current);

    Result queryBlogById(Long id);

    /**
     * 查询某店铺下的探店笔记（按点赞数倒序，店铺详情页聚合展示）
     */
    Result queryBlogByShopId(Long shopId);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);

    Result deleteBlog(Long id);

    /**
     * 笔记评论数 +1（发布评论后同步）
     */
    void incrCommentCount(Long blogId);

    /**
     * 笔记评论数 -count（删除评论后同步，GREATEST 保底防止负数）
     */
    void decrCommentCount(Long blogId, long count);

}
