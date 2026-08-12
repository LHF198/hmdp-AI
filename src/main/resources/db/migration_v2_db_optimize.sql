-- ============================================================
-- hmdp 数据库增量迁移脚本（V2：数据库优化整改）
-- 适用场景：已通过 hmdp.sql 建过表的存量环境
-- 说明：
--   1. MySQL 5.7 不支持 ADD INDEX IF NOT EXISTS，本脚本仅可执行一次；
--      重复执行会报 Duplicate key name，可忽略已存在的索引报错继续
--   2. 全新环境请直接使用更新后的 db/hmdp.sql，无需执行本脚本
-- 日期：2026-08
-- ============================================================

USE `hmdp`;

-- ------------------------------------------------------------
-- 1. tb_shop：补齐 city 列（与代码 .eq("city") 查询对齐；已存在该列的环境跳过本段）
-- ------------------------------------------------------------
-- ALTER TABLE `tb_shop`
--     ADD COLUMN `city` varchar(64) NOT NULL DEFAULT '' COMMENT '店铺所在城市' AFTER `name`;
-- UPDATE `tb_shop` SET `city` = '杭州' WHERE `city` = '';

-- ------------------------------------------------------------
-- 2. tb_shop：类型 + 城市列表筛选组合索引（queryShopByType 人气/评分排序走 DB 分支）
-- ------------------------------------------------------------
ALTER TABLE `tb_shop` ADD INDEX `idx_type_city`(`type_id`, `city`);

-- ------------------------------------------------------------
-- 3. tb_voucher_order：一人一单 DB 层唯一保险 + 我的订单列表(user_id + create_time)
-- ------------------------------------------------------------
ALTER TABLE `tb_voucher_order`
    ADD UNIQUE KEY `uk_user_voucher`(`user_id`, `voucher_id`),
    ADD INDEX `idx_user_time`(`user_id`, `create_time`);

-- ------------------------------------------------------------
-- 4. tb_blog_comments：评论列表/计数(blog_id)、我的评论(user_id)、回复级联(parent_id)
-- ------------------------------------------------------------
ALTER TABLE `tb_blog_comments`
    ADD INDEX `idx_blog`(`blog_id`, `create_time`),
    ADD INDEX `idx_user`(`user_id`),
    ADD INDEX `idx_parent`(`parent_id`);

-- ------------------------------------------------------------
-- 5. tb_blog：热门榜按点赞数排序
-- ------------------------------------------------------------
ALTER TABLE `tb_blog` ADD INDEX `idx_liked`(`liked`);

-- ------------------------------------------------------------
-- 6. tb_follow：粉丝列表查询（现有唯一索引前缀是 user_id，无法用于 follow_user_id 查询）
-- ------------------------------------------------------------
ALTER TABLE `tb_follow` ADD INDEX `idx_fans`(`follow_user_id`);
