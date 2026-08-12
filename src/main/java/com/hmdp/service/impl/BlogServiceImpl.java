package com.hmdp.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 笔记服务实现类：发布笔记并 Push 到粉丝 Feed 收件箱（Redis ZSet）、 点赞/取消点赞（ZSet
 * 记录并排序）、滚动分页查询关注的人的笔记
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    @Resource
    private IBlogCommentsService blogCommentsService;

    /**
     * feed 流推送线程池。使用 daemon 线程：应用关闭时不会阻止 JVM 退出。
     * 发布笔记后异步推送给粉丝，粉丝量大时不阻塞发布接口（单粉丝推送失败不影响主流程）。
     */
    private static final ExecutorService FEED_PUSH_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "feed-push-worker");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void destroy() {
        FEED_PUSH_EXECUTOR.shutdownNow();
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id) {
        // 1.查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        // 2.查询blog有关的用户
        queryBlogUser(blog);
        // 3.查询blog是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        // 2.判断当前登录用户是否已经点赞
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result likeBlog(Long id) {
        //1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        //2.判断当前用户是否已经点赞
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            //3.如果没有点赞
            //3.1数据库点赞 +1
            boolean isSuccess = update().setSql("liked=liked+1").eq("id", id).update();
            if (isSuccess) {
                //3.2保存用户到redis的set集合
                try {
                    stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
                } catch (Exception e) {
                    // Redis 写入失败：回滚 DB 计数，保持 DB/Redis 状态一致
                    log.error("点赞缓存写入失败，回滚DB计数: blogId={}, userId={}", id, userId, e);
                    update().setSql("liked = IF(liked > 0, liked - 1, 0)").eq("id", id).update();
                }
            }
        } else {
            //4.如果已经点赞
            //4.1.数据库点赞-1（IF 防下溢：liked 为 int UNSIGNED，0 时再减会直接报错）
            boolean isSuccess = update().setSql("liked = IF(liked > 0, liked - 1, 0)").eq("id", id).update();
            if (isSuccess) {
                //4.2.取消redis的点赞
                try {
                    stringRedisTemplate.opsForZSet().remove(key, userId.toString());
                } catch (Exception e) {
                    // Redis 删除失败：回滚 DB 计数，保持 DB/Redis 状态一致
                    log.error("点赞缓存删除失败，回滚DB计数: blogId={}, userId={}", id, userId, e);
                    update().setSql("liked=liked+1").eq("id", id).update();
                }
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 1.查询top5的点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 2.解析出其中的用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        String idsStr = StrUtil.join(",", ids);
        // 3.根据用户id查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids).last("ORDER BY FIELD(id," + idsStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 4.返回
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 1.5 确保 images 不为空（MySQL 5.7 TEXT 字段不支持默认值）
        if (blog.getImages() == null) {
            blog.setImages("");
        }
        // 2.保存探店笔记
        boolean isSuccess = save(blog);
        if (!isSuccess) {
            return Result.fail("新增笔记失败!");
        }
        // 3.查询笔记作者的所有粉丝 select * from tb_follow where follow_user_id = ?
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        // 4.异步推送笔记id给所有粉丝（不阻塞发布接口；粉丝量大时由线程池排队处理）
        FEED_PUSH_EXECUTOR.submit(() -> {
            try {
                for (Follow follow : follows) {
                    // 4.1.获取粉丝id
                    Long userId = follow.getUserId();
                    // 4.2.推送
                    String key = FEED_KEY + userId;
                    stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
                }
            } catch (Exception e) {
                log.error("推送笔记到粉丝feed流失败: blogId={}", blog.getId(), e);
            }
        });
        // 5.返回id
        return Result.ok(blog.getId());
    }

    @Override
    @Transactional
    public Result deleteBlog(Long id) {
        // 1.获取登录用户（/blog/* 已免登录放行，此处兜底校验）
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        // 2.查询笔记
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        // 3.校验是否为本人笔记
        if (!blog.getUserId().equals(user.getId())) {
            return Result.fail("只能删除自己的笔记！");
        }
        // 4.删除笔记下的所有评论
        blogCommentsService.remove(new LambdaQueryWrapper<BlogComments>().eq(BlogComments::getBlogId, id));
        // 5.删除笔记点赞缓存
        stringRedisTemplate.delete(BLOG_LIKED_KEY + id);
        // 6.从所有粉丝的feed流中移除该笔记
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        for (Follow follow : follows) {
            stringRedisTemplate.opsForZSet().remove(FEED_KEY + follow.getUserId(), id.toString());
        }
        // 7.删除笔记关联的图片文件（失败不影响主流程）
        if (StrUtil.isNotBlank(blog.getImages())) {
            for (String img : blog.getImages().split(",")) {
                if (StrUtil.isBlank(img) || !img.startsWith("/imgs/")) {
                    continue;
                }
                try {
                    FileUtil.del(SystemConstants.IMAGE_UPLOAD_DIR + StrUtil.removePrefix(img, "/imgs/"));
                } catch (Exception e) {
                    log.error("删除笔记图片失败: {}", img, e);
                }
            }
        }
        // 8.删除笔记记录
        boolean isSuccess = removeById(id);
        if (!isSuccess) {
            return Result.fail("删除笔记失败！");
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
        String key = FEED_KEY + userId;
        //滚动分页查询，max是上一次查询的最小分数，offset是偏移量（降序查询）
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        // 3.非空判断
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        // 4.解析数据：blogId、minTime（时间戳）、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0; // 2
        int os = 1; // 2
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) { // 5 4 4 2 2
            // 4.1.获取id
            ids.add(Long.valueOf(tuple.getValue()));
            // 4.2.获取分数(时间戳）
            long time = tuple.getScore().longValue();
            // 4.3.如果时间戳与上一条记录时间戳相同，则偏移量+1，反之则重置
            if (time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }

        // 5.根据id查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();

        for (Blog blog : blogs) {
            // 5.1.查询blog有关的用户
            queryBlogUser(blog);
            // 5.2.查询blog是否被点赞
            isBlogLiked(blog);
        }

        // 6.封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);

        return Result.ok(r);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
