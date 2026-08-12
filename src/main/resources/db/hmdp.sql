/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 50622
 Source Host           : localhost:3306
 Source Schema         : hmdp2

 Target Server Type    : MySQL
 Target Server Version : 50622
 File Encoding         : 65001

 Date: 02/03/2022 23:12:54
*/

-- 首次导入自动建库（与 application.yaml 中的库名 hmdp 保持一致）
CREATE DATABASE IF NOT EXISTS `hmdp` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `hmdp`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tb_blog
-- ----------------------------
DROP TABLE IF EXISTS `tb_blog`;
CREATE TABLE `tb_blog`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id` bigint(20) NOT NULL COMMENT '商户id',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `images` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '探店的照片，最多9张，多张以\",\"隔开',
  `content` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '探店的文字描述',
  `liked` int(8) UNSIGNED NULL DEFAULT 0 COMMENT '点赞数量',
  `comments` int(8) UNSIGNED NULL DEFAULT NULL COMMENT '评论数量',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_liked`(`liked`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_blog
-- ----------------------------
INSERT INTO `tb_blog` VALUES (4, 4, 2, '无尽浪漫的夜晚丨在万花丛中摇晃着红酒杯🍷品战斧牛排🥩', '/imgs/blogs/7/14/4771fefb-1a87-4252-816c-9f7ec41ffa4a.jpg,/imgs/blogs/4/10/2f07e3c9-ddce-482d-9ea7-c21450f8d7cd.jpg,/imgs/blogs/2/6/b0756279-65da-4f2d-b62a-33f74b06454a.jpg,/imgs/blogs/10/7/7e97f47d-eb49-4dc9-a583-95faa7aed287.jpg,/imgs/blogs/1/2/4a7b496b-2a08-4af7-aa95-df2c3bd0ef97.jpg,/imgs/blogs/14/3/52b290eb-8b5d-403b-8373-ba0bb856d18e.jpg', '生活就是一半烟火·一半诗意\n手执烟火谋生活·心怀诗意以谋爱·\n当然 男朋友给不了的浪漫要学会自己给🍒\n无法重来的一生·尽量快乐.\n\n🏰「小筑里·神秘浪漫花园餐厅」🏰\n\n💯这是一家最最最美花园的西餐厅·到处都是花餐桌上是花前台是花  美好无处不在\n品一口葡萄酒，维亚红酒马瑟兰·微醺上头工作的疲惫消失无际·生如此多娇🍃\n\n📍地址:延安路200号(家乐福面)\n\n🚌交通:地铁①号线定安路B口出右转过下通道右转就到啦～\n\n--------------🥣菜品详情🥣---------------\n\n「战斧牛排」\n超大一块战斧牛排经过火焰的炙烤发出阵阵香，外焦里嫩让人垂涎欲滴，切开牛排的那一刻，牛排的汁水顺势流了出来，分熟的牛排肉质软，简直细嫩到犯规，一刻都等不了要放入嘴里咀嚼～\n\n「奶油培根意面」\n太太太好吃了💯\n我真的无法形容它的美妙，意面混合奶油香菇的香味真的太太太香了，我真的舔盘了，一丁点美味都不想浪费‼️\n\n「香菜汁烤鲈鱼」\n这个酱是辣的 真的绝好吃‼️\n鲈鱼本身就很嫩没什么刺，烤过之后外皮酥酥的，鱼肉蘸上酱料根本停不下来啊啊啊啊\n能吃辣椒的小伙伴一定要尝尝\n\n 非常可 好吃子🍽\n\n--------------🍃个人感受🍃---------------\n\n【👩🏻‍🍳服务】\n小姐姐特别耐心的给我们介绍彩票 推荐特色菜品，拍照需要帮忙也是尽心尽力配合，太爱他们了\n\n【🍃环境】\n比较有格调的西餐厅 整个餐厅的布局可称得上的万花丛生 有种在人间仙境的感觉🌸\n集美食美酒与鲜花为一体的风格店铺 令人向往\n烟火皆是生活 人间皆是浪漫', 00000013, 00000107, '2021-12-28 19:50:01', '2022-01-06 20:30:03');
INSERT INTO `tb_blog` VALUES (5, 1, 2, '人均30💰杭州这家港式茶餐厅我疯狂打call‼️', '/imgs/blogs/4/7/863cc302-d150-420d-a596-b16e9232a1a6.jpg,/imgs/blogs/11/12/8b37d208-9414-4e78-b065-9199647bb3e3.jpg,/imgs/blogs/4/1/fa74a6d6-3026-4cb7-b0b6-35abb1e52d11.jpg,/imgs/blogs/9/12/ac2ce2fb-0605-4f14-82cc-c962b8c86688.jpg,/imgs/blogs/4/0/26a7cd7e-6320-432c-a0b4-1b7418f45ec7.jpg,/imgs/blogs/15/9/cea51d9b-ac15-49f6-b9f1-9cf81e9b9c85.jpg', '又吃到一家好吃的茶餐厅🍴环境是怀旧tvb港风📺边吃边拍照片📷几十种菜品均价都在20+💰可以是很平价了！\n·\n店名：九记冰厅(远洋店)\n地址：杭州市丽水路远洋乐堤港负一楼（溜冰场旁边）\n·\n✔️黯然销魂饭（38💰）\n这碗饭我吹爆！米饭上盖满了甜甜的叉烧 还有两颗溏心蛋🍳每一粒米饭都裹着浓郁的酱汁 光盘了\n·\n✔️铜锣湾漏奶华（28💰）\n黄油吐司烤的脆脆的 上面洒满了可可粉🍫一刀切开 奶盖流心像瀑布一样流出来  满足\n·\n✔️神仙一口西多士士（16💰）\n简简单单却超级好吃！西多士烤的很脆 黄油味浓郁 面包体超级柔软 上面淋了炼乳\n·\n✔️怀旧五柳炸蛋饭（28💰）\n四个鸡蛋炸成蓬松的炸蛋！也太好吃了吧！还有大块鸡排 上淋了酸甜的酱汁 太合我胃口了！！\n·\n✔️烧味双拼例牌（66💰）\n选了烧鹅➕叉烧 他家烧腊品质真的惊艳到我！据说是每日广州发货 到店现烧现卖的黑棕鹅 每口都是正宗的味道！肉质很嫩 皮超级超级酥脆！一口爆油！叉烧肉也一点都不柴 甜甜的很入味 搭配梅子酱很解腻 ！\n·\n✔️红烧脆皮乳鸽（18.8💰）\n乳鸽很大只 这个价格也太划算了吧， 肉质很有嚼劲 脆皮很酥 越吃越香～\n·\n✔️大满足小吃拼盘（25💰）\n翅尖➕咖喱鱼蛋➕蝴蝶虾➕盐酥鸡\nzui喜欢里面的咖喱鱼！咖喱酱香甜浓郁！鱼蛋很q弹～\n·\n✔️港式熊仔丝袜奶茶（19💰）\n小熊🐻造型的奶茶冰也太可爱了！颜值担当 很地道的丝袜奶茶 茶味特别浓郁～\n·', 00000002, 00000002, '2021-12-28 20:57:49', '2022-01-06 20:30:22');
INSERT INTO `tb_blog` VALUES (6, 10, 1, '杭州周末好去处｜💰50就可以骑马啦🐎', '/imgs/blogs/blog1.jpg', '杭州周末好去处｜💰50就可以骑马啦🐎', 00000011, 00000002, '2022-01-11 16:05:47', '2022-01-11 16:05:47');
INSERT INTO `tb_blog` VALUES (7, 10, 1, '杭州周末好去处｜💰50就可以骑马啦🐎', '/imgs/blogs/blog1.jpg', '杭州周末好去处｜💰50就可以骑马啦🐎', 00000011, 00000000, '2022-01-11 16:05:47', '2022-01-11 16:05:47');
INSERT INTO `tb_blog` VALUES (8, 34, 7, '杭州夜生活指南｜人均80的海伦司微醺之夜🍻', '/imgs/blogs/7/14/4771fefb-1a87-4252-816c-9f7ec41ffa4a.jpg,/imgs/blogs/4/10/2f07e3c9-ddce-482d-9ea7-c21450f8d7cd.jpg', '谁说小酒馆一定很贵？
运河商厦楼下这家海伦司，人均80就能喝到微醺！

🍻点单攻略：
• 啤酒超级平价，小瓶装才几块钱
• 还有各种果酒和洋酒套餐
• 小吃拼盘也才20多

📷拍照位：
进门右侧的霓虹灯墙真的超好拍，随便一拍都是大片感！

晚上9点后人超多，想安静喝酒的建议早点去～', 00000023, 00000002, '2022-01-12 20:30:00', '2022-01-12 20:30:00');
INSERT INTO `tb_blog` VALUES (9, 32, 8, '遛娃天花板！Meland Club玩到不想回家🎠', '/imgs/blogs/blog1.jpg', '带娃打卡了城西银泰的Meland Club，真的是室内遛娃的天花板！

🎡近6000平的机械探索乐园：
• 三层楼高的超级迷宫滑梯
• 纯白海洋球池，拍照绝美
• 模拟城市区可以体验各种职业
• 亲子餐厅玩累了直接吃

💰票价参考：一大一小298元，工作日去人少体验更好！

⏰营业时间：周一10:00-18:00，周二至周日10:00-21:00

温馨提示：记得自带防滑袜，店里买5元一双～', 00000045, 00000002, '2022-01-12 21:00:00', '2022-01-12 21:00:00');
INSERT INTO `tb_blog` VALUES (10, 22, 10, '拱墅区宝藏健身房｜24小时自助撸铁打卡💪', '/imgs/blogs/blog1.jpg', '运河上街这家乐刻真的爱了！24小时自助，随时都能去练！

🏋️器械：
• 自由力量区器械很全
• 有氧区跑步机椭圆机都带电视
• 团课教室每天都有课表

💰价格：月卡99元起，比传统健身房便宜太多！

📍地址：衢州路2-38号（运河上街）

练完还能去楼上吃个饭，完美～', 00000018, 00000002, '2022-01-13 09:00:00', '2022-01-13 09:00:00');
INSERT INTO `tb_blog` VALUES (11, 15, 3, '绿茶餐厅人均90吃到扶墙出🍵', '/imgs/blogs/blog1.jpg', '乐堤港的绿茶餐厅，环境是江南园林风，小桥流水超有意境！

🍵必点菜单：
• 面包诱惑：吐司烤得外酥里嫩，蘸冰淇淋绝了
• 绿茶烤鸡：皮脆肉嫩，招牌必点
• 石锅鸡汤豆腐：超级下饭
• 干锅花菜：家常味道

💰人均90，性价比很高！周末等位1小时起，建议提前去～', 00000036, 00000000, '2022-01-13 12:30:00', '2022-01-13 12:30:00');
INSERT INTO `tb_blog` VALUES (12, 25, 6, '加班人的续命秘籍｜足道按摩初体验🧖', '/imgs/blogs/blog1.jpg', '连轴转了一周，终于来密渡桥路的臻本足道放松了一下！

💆体验感受：
• 环境很安静，有独立的包间
• 技师手法很专业，力道可以随时调整
• 还有水果茶点供应，体验感拉满

💰人均139，性价比不错

⚠️建议提前电话预约，晚上人很多～

打工人真的要学会对自己好一点！', 00000012, 00000000, '2022-01-13 19:00:00', '2022-01-13 19:00:00');
INSERT INTO `tb_blog` VALUES (13, 37, 9, '周末的正确打开方式｜胡桃里音乐酒馆🎵', '/imgs/blogs/blog1.jpg', '东文街的胡桃里，白天是餐厅，晚上是音乐酒馆，氛围感绝了！

🎶晚上有驻唱歌手表演，从民谣到流行都有
🍽️菜品推荐：胡桃里烤鸡半只68元，皮脆多汁
🥂还有各种特调鸡尾酒，微醺刚刚好

💰人均150，适合情侣约会和朋友聚会

建议晚上8点以后去，现场氛围最棒！', 00000028, 00000000, '2022-01-14 20:00:00', '2022-01-14 20:00:00');
INSERT INTO `tb_blog` VALUES (14, 29, 7, '美容SPA初体验｜被思妍丽治愈了✨', '/imgs/blogs/blog1.jpg', '华浙广场的思妍丽，朋友安利了很久终于来体验了！

💆项目体验：
• 提前预约了全身SPA，进门先做皮肤检测
• 护理师手法超专业，全程没有推销
• 独立护理室，私密性很好
• 结束后还有养生粥和小点心

💰人均398，贵是贵了点，但体验确实值

一周的疲惫都被治愈了～', 00000009, 00000000, '2022-01-14 15:00:00', '2022-01-14 15:00:00');
INSERT INTO `tb_blog` VALUES (15, 41, 2, '人均98的轰趴馆｜公司团建天花板🎉', '/imgs/blogs/blog1.jpg', '上周公司团建去了拱墅万达的漆橙车间轰趴馆，太快乐了！

🎮娱乐设施：
• KTV包厢音响很专业
• 台球、桌游、街机应有尽有
• 还有Switch和Xbox

🍲餐饮：火锅烧烤套餐都有，吃到撑

💰人均98，性价比超高！

📍地址：杭行路666号拱墅万达广场

适合20人以内的小团体聚会，记得提前预约～', 00000031, 00000001, '2022-01-15 14:00:00', '2022-01-15 14:00:00');
INSERT INTO `tb_blog` VALUES (16, 26, 1, '三立开元酒店的良子足浴，放松首选💆', '/imgs/blogs/blog1.jpg', '出差回来腿都是肿的，朋友推荐了绍兴路的良子足浴！

💆环境：在三立开元名都大酒店4层，环境很高档
👐手法：技师力道到位，按完浑身轻松
🍵服务：还有茶水和小食

💰人均158，和普通足浴店比贵一点，但环境真的值

顺便夸一句：酒店停车很方便！', 00000015, 00000000, '2022-01-15 20:30:00', '2022-01-15 20:30:00');

-- ----------------------------
-- Table structure for tb_blog_comments
-- ----------------------------
DROP TABLE IF EXISTS `tb_blog_comments`;
CREATE TABLE `tb_blog_comments`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `blog_id` bigint(20) UNSIGNED NOT NULL COMMENT '探店id',
  `parent_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联的1级评论id，如果是一级评论，则值为0',
  `answer_id` bigint(20) UNSIGNED NOT NULL COMMENT '回复的评论id',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '回复的内容',
  `liked` int(8) UNSIGNED NULL DEFAULT NULL COMMENT '点赞数',
  `status` tinyint(1) UNSIGNED NULL DEFAULT NULL COMMENT '状态，0：正常，1：被举报，2：禁止查看',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_blog`(`blog_id`, `create_time`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_parent`(`parent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_blog_comments
-- ----------------------------
INSERT INTO `tb_blog_comments` VALUES (1, 3, 4, 0, 0, '这家店太适合约会了，环境绝美！', 12, 0, '2022-01-05 12:00:00', '2022-01-05 12:00:00');
INSERT INTO `tb_blog_comments` VALUES (2, 7, 4, 0, 0, '拍照超好看，光线绝了📸', 8, 0, '2022-01-05 13:00:00', '2022-01-05 13:00:00');
INSERT INTO `tb_blog_comments` VALUES (3, 6, 4, 0, 0, '战斧牛排yyds！已种草', 15, 0, '2022-01-06 10:00:00', '2022-01-06 10:00:00');
INSERT INTO `tb_blog_comments` VALUES (4, 3, 5, 0, 0, '黯然销魂饭是真的绝，人均30良心价！', 20, 0, '2022-01-06 14:00:00', '2022-01-06 14:00:00');
INSERT INTO `tb_blog_comments` VALUES (5, 1, 5, 0, 0, '下次组队去吃！', 6, 0, '2022-01-07 09:00:00', '2022-01-07 09:00:00');
INSERT INTO `tb_blog_comments` VALUES (6, 9, 6, 0, 0, '50块骑马也太划算了吧！', 10, 0, '2022-01-12 10:00:00', '2022-01-12 10:00:00');
INSERT INTO `tb_blog_comments` VALUES (7, 10, 6, 0, 0, '周末就去打卡！', 5, 0, '2022-01-12 11:00:00', '2022-01-12 11:00:00');
INSERT INTO `tb_blog_comments` VALUES (8, 9, 8, 0, 0, '海伦司真的是平价快乐源泉！', 18, 0, '2022-01-12 21:00:00', '2022-01-12 21:00:00');
INSERT INTO `tb_blog_comments` VALUES (9, 1, 8, 0, 0, '收藏了，周五约起！', 7, 0, '2022-01-13 09:00:00', '2022-01-13 09:00:00');
INSERT INTO `tb_blog_comments` VALUES (10, 6, 9, 0, 0, '周末就带娃去！', 9, 0, '2022-01-13 10:00:00', '2022-01-13 10:00:00');
INSERT INTO `tb_blog_comments` VALUES (11, 3, 9, 0, 0, '门票有点贵，但确实值得', 4, 0, '2022-01-13 11:00:00', '2022-01-13 11:00:00');
INSERT INTO `tb_blog_comments` VALUES (12, 7, 10, 0, 0, '器械全吗？求具体地址', 3, 0, '2022-01-13 12:00:00', '2022-01-13 12:00:00');
INSERT INTO `tb_blog_comments` VALUES (13, 2, 10, 0, 0, '24小时营业也太方便了吧', 6, 0, '2022-01-13 13:00:00', '2022-01-13 13:00:00');
INSERT INTO `tb_blog_comments` VALUES (14, 8, 15, 0, 0, '团建好去处，收藏了！', 5, 0, '2022-01-16 09:00:00', '2022-01-16 09:00:00');

-- ----------------------------
-- Table structure for tb_follow
-- ----------------------------
DROP TABLE IF EXISTS `tb_follow`;
CREATE TABLE `tb_follow`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `follow_user_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联的用户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`) USING BTREE,
  INDEX `idx_fans`(`follow_user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_follow
-- ----------------------------
INSERT INTO `tb_follow` VALUES (1, 1, 2, '2022-01-05 10:00:00');
INSERT INTO `tb_follow` VALUES (2, 1, 3, '2022-01-05 10:05:00');
INSERT INTO `tb_follow` VALUES (3, 1, 7, '2022-01-10 09:00:00');
INSERT INTO `tb_follow` VALUES (4, 2, 1, '2021-12-25 12:00:00');
INSERT INTO `tb_follow` VALUES (5, 2, 3, '2021-12-28 20:00:00');
INSERT INTO `tb_follow` VALUES (6, 2, 7, '2022-01-09 10:00:00');
INSERT INTO `tb_follow` VALUES (7, 2, 8, '2022-01-10 15:00:00');
INSERT INTO `tb_follow` VALUES (8, 3, 1, '2021-12-26 09:00:00');
INSERT INTO `tb_follow` VALUES (9, 3, 7, '2022-01-09 11:00:00');
INSERT INTO `tb_follow` VALUES (10, 6, 1, '2022-01-08 13:00:00');
INSERT INTO `tb_follow` VALUES (11, 6, 8, '2022-01-10 16:00:00');
INSERT INTO `tb_follow` VALUES (12, 7, 1, '2022-01-09 10:30:00');
INSERT INTO `tb_follow` VALUES (13, 7, 2, '2022-01-09 10:40:00');
INSERT INTO `tb_follow` VALUES (14, 8, 1, '2022-01-10 14:30:00');
INSERT INTO `tb_follow` VALUES (15, 8, 7, '2022-01-10 15:30:00');
INSERT INTO `tb_follow` VALUES (16, 9, 1, '2022-01-11 21:00:00');
INSERT INTO `tb_follow` VALUES (17, 9, 7, '2022-01-12 21:30:00');
INSERT INTO `tb_follow` VALUES (18, 10, 1, '2022-01-12 09:00:00');
INSERT INTO `tb_follow` VALUES (19, 10, 7, '2022-01-12 09:10:00');

-- ----------------------------
-- Table structure for tb_seckill_voucher
-- ----------------------------
DROP TABLE IF EXISTS `tb_seckill_voucher`;
CREATE TABLE `tb_seckill_voucher`  (
  `voucher_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联的优惠券的id',
  `stock` int(8) NOT NULL COMMENT '库存',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `begin_time` timestamp NULL DEFAULT NULL COMMENT '生效时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '失效时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`voucher_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '秒杀优惠券表，与优惠券是一对一关系' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_seckill_voucher
-- ----------------------------
INSERT INTO `tb_seckill_voucher` VALUES (3, 100, '2022-01-05 10:00:00', '2022-01-01 00:00:00', '2022-12-31 23:59:59', '2022-01-05 10:00:00');
INSERT INTO `tb_seckill_voucher` VALUES (4, 200, '2022-01-05 10:00:00', '2022-01-01 00:00:00', '2022-12-31 23:59:59', '2022-01-05 10:00:00');
INSERT INTO `tb_seckill_voucher` VALUES (6, 150, '2022-01-07 14:00:00', '2022-01-01 00:00:00', '2022-12-31 23:59:59', '2022-01-07 14:00:00');
INSERT INTO `tb_seckill_voucher` VALUES (8, 80, '2022-01-07 13:00:00', '2022-01-01 00:00:00', '2022-12-31 23:59:59', '2022-01-07 13:00:00');

-- ----------------------------
-- Table structure for tb_shop
-- ----------------------------
DROP TABLE IF EXISTS `tb_shop`;
CREATE TABLE `tb_shop`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商铺名称',
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '店铺所在城市',
  `type_id` bigint(20) UNSIGNED NOT NULL COMMENT '商铺类型的id',
  `images` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商铺图片，多个图片以\',\'隔开',
  `area` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商圈，例如陆家嘴',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '地址',
  `x` double UNSIGNED NOT NULL COMMENT '经度',
  `y` double UNSIGNED NOT NULL COMMENT '维度',
  `avg_price` bigint(10) UNSIGNED NULL DEFAULT NULL COMMENT '均价，取整数',
  `sold` int(10) UNSIGNED NOT NULL COMMENT '销量',
  `comments` int(10) UNSIGNED NOT NULL COMMENT '评论数量',
  `score` int(2) UNSIGNED NOT NULL COMMENT '评分，1~5分，乘10保存，避免小数',
  `open_hours` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '营业时间，例如 10:00-22:00',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `foreign_key_type`(`type_id`) USING BTREE,
  INDEX `idx_type_city`(`type_id`, `city`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_shop
-- ----------------------------
INSERT INTO `tb_shop` VALUES (1, '103茶餐厅', 1, 'https://qcloud.dpfile.com/pc/jiclIsCKmOI2arxKN1Uf0Hx3PucIJH8q0QSz-Z8llzcN56-_QiKuOvyio1OOxsRtFoXqu0G3iT2T27qat3WhLVEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vfCF2ubeXzk49OsGrXt_KYDCngOyCwZK-s3fqawWswzk.jpg,https://qcloud.dpfile.com/pc/IOf6VX3qaBgFXFVgp75w-KKJmWZjFc8GXDU8g9bQC6YGCpAmG00QbfT4vCCBj7njuzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg', '大关', '金华路锦昌文华苑29号', 120.149192, 30.316078, 80, 0000004215, 0000003035, 37, '10:00-22:00', '2021-12-22 18:10:39', '2022-01-13 17:32:19');
INSERT INTO `tb_shop` VALUES (2, '蔡馬洪涛烤肉·老北京铜锅涮羊肉', 1, 'https://p0.meituan.net/bbia/c1870d570e73accbc9fee90b48faca41195272.jpg,http://p0.meituan.net/mogu/397e40c28fc87715b3d5435710a9f88d706914.jpg,https://qcloud.dpfile.com/pc/MZTdRDqCZdbPDUO0Hk6lZENRKzpKRF7kavrkEI99OxqBZTzPfIxa5E33gBfGouhFuzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg', '拱宸桥/上塘', '上塘路1035号（中国工商银行旁）', 120.151505, 30.333422, 85, 0000002160, 0000001460, 46, '11:30-03:00', '2021-12-22 19:00:13', '2022-01-11 16:12:26');
INSERT INTO `tb_shop` VALUES (3, '新白鹿餐厅(运河上街店)', 1, 'https://p0.meituan.net/biztone/694233_1619500156517.jpeg,https://img.meituan.net/msmerchant/876ca8983f7395556eda9ceb064e6bc51840883.png,https://img.meituan.net/msmerchant/86a76ed53c28eff709a36099aefe28b51554088.png', '运河上街', '台州路2号运河上街购物中心F5', 120.151954, 30.32497, 61, 0000012035, 0000008045, 47, '10:30-21:00', '2021-12-22 19:10:05', '2022-01-11 16:12:42');
INSERT INTO `tb_shop` VALUES (4, 'Mamala(杭州远洋乐堤港店)', 1, 'https://img.meituan.net/msmerchant/232f8fdf09050838bd33fb24e79f30f9606056.jpg,https://qcloud.dpfile.com/pc/rDe48Xe15nQOHCcEEkmKUp5wEKWbimt-HDeqYRWsYJseXNncvMiXbuED7x1tXqN4uzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg', '拱宸桥/上塘', '丽水路66号远洋乐堤港商城2期1层B115号', 120.146659, 30.312742, 290, 0000013519, 0000009529, 49, '11:00-22:00', '2021-12-22 19:17:15', '2022-01-11 16:12:51');
INSERT INTO `tb_shop` VALUES (5, '海底捞火锅(水晶城购物中心店）', 1, 'https://img.meituan.net/msmerchant/054b5de0ba0b50c18a620cc37482129a45739.jpg,https://img.meituan.net/msmerchant/59b7eff9b60908d52bd4aea9ff356e6d145920.jpg,https://qcloud.dpfile.com/pc/Qe2PTEuvtJ5skpUXKKoW9OQ20qc7nIpHYEqJGBStJx0mpoyeBPQOJE4vOdYZwm9AuzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg', '大关', '上塘路458号水晶城购物中心F6', 120.15778, 30.310633, 104, 0000004125, 0000002764, 49, '10:00-07:00', '2021-12-22 19:20:58', '2022-01-11 16:13:01');
INSERT INTO `tb_shop` VALUES (6, '幸福里老北京涮锅（丝联店）', 1, 'https://img.meituan.net/msmerchant/e71a2d0d693b3033c15522c43e03f09198239.jpg,https://img.meituan.net/msmerchant/9f8a966d60ffba00daf35458522273ca658239.jpg,https://img.meituan.net/msmerchant/ef9ca5ef6c05d381946fe4a9aa7d9808554502.jpg', '拱宸桥/上塘', '金华南路189号丝联166号', 120.148603, 30.318618, 130, 0000009531, 0000007324, 46, '11:00-13:50,17:00-20:50', '2021-12-22 19:24:53', '2022-01-11 16:13:09');
INSERT INTO `tb_shop` VALUES (7, '炉鱼(拱墅万达广场店)', 1, 'https://img.meituan.net/msmerchant/909434939a49b36f340523232924402166854.jpg,https://img.meituan.net/msmerchant/32fd2425f12e27db0160e837461c10303700032.jpg,https://img.meituan.net/msmerchant/f7022258ccb8dabef62a0514d3129562871160.jpg', '北部新城', '杭行路666号万达商业中心4幢2单元409室(铺位号4005)', 120.124691, 30.336819, 85, 0000002631, 0000001320, 47, '00:00-24:00', '2021-12-22 19:40:52', '2022-01-11 16:13:19');
INSERT INTO `tb_shop` VALUES (8, '浅草屋寿司（运河上街店）', 1, 'https://img.meituan.net/msmerchant/cf3dff697bf7f6e11f4b79c4e7d989e4591290.jpg,https://img.meituan.net/msmerchant/0b463f545355c8d8f021eb2987dcd0c8567811.jpg,https://img.meituan.net/msmerchant/c3c2516939efaf36c4ccc64b0e629fad587907.jpg', '运河上街', '拱墅区金华路80号运河上街B1', 120.150526, 30.325231, 88, 0000002406, 0000001206, 46, ' 11:00-21:30', '2021-12-22 19:51:06', '2022-01-11 16:13:25');
INSERT INTO `tb_shop` VALUES (9, '羊老三羊蝎子牛仔排北派炭火锅(运河上街店)', 1, 'https://p0.meituan.net/biztone/163160492_1624251899456.jpeg,https://img.meituan.net/msmerchant/e478eb16f7e31a7f8b29b5e3bab6de205500837.jpg,https://img.meituan.net/msmerchant/6173eb1d18b9d70ace7fdb3f2dd939662884857.jpg', '运河上街', '台州路2号运河上街购物中心F5', 120.150598, 30.325251, 101, 0000002763, 0000001363, 44, '11:00-21:30', '2021-12-22 19:53:59', '2022-01-11 16:13:34');
INSERT INTO `tb_shop` VALUES (10, '开乐迪KTV（运河上街店）', 2, 'https://p0.meituan.net/joymerchant/a575fd4adb0b9099c5c410058148b307-674435191.jpg,https://p0.meituan.net/merchantpic/68f11bf850e25e437c5f67decfd694ab2541634.jpg,https://p0.meituan.net/dpdeal/cb3a12225860ba2875e4ea26c6d14fcc197016.jpg', '运河上街', '台州路2号运河上街购物中心F4', 120.149093, 30.324666, 67, 0000026891, 0000000902, 37, '00:00-24:00', '2021-12-22 20:25:16', '2021-12-22 20:25:16');
INSERT INTO `tb_shop` VALUES (11, 'INLOVE KTV(水晶城店)', 2, 'https://p0.meituan.net/dpmerchantpic/53e74b200211d68988a4f02ae9912c6c1076826.jpg,https://qcloud.dpfile.com/pc/4iWtIvzLzwM2MGgyPu1PCDb4SWEaKqUeHm--YAt1EwR5tn8kypBcqNwHnjg96EvT_Gd2X_f-v9T8Yj4uLt25Gg.jpg,https://qcloud.dpfile.com/pc/WZsJWRI447x1VG2x48Ujgu7vwqksi_9WitdKI4j3jvIgX4MZOpGNaFtM93oSSizbGybIjx5eX6WNgCPvcASYAw.jpg', '水晶城', '上塘路458号水晶城购物中心6层', 120.15853, 30.310002, 75, 0000035977, 0000005684, 47, '11:30-06:00', '2021-12-22 20:29:02', '2021-12-22 20:39:00');
INSERT INTO `tb_shop` VALUES (12, '魅(杭州远洋乐堤港店)', 2, 'https://p0.meituan.net/dpmerchantpic/63833f6ba0393e2e8722420ef33f3d40466664.jpg,https://p0.meituan.net/dpmerchantpic/ae3c94cc92c529c4b1d7f68cebed33fa105810.png,', '远洋乐堤港', '丽水路58号远洋乐堤港F4', 120.14983, 30.31211, 88, 0000006444, 0000000235, 46, '10:00-02:00', '2021-12-22 20:34:34', '2021-12-22 20:34:34');
INSERT INTO `tb_shop` VALUES (13, '讴K拉量贩KTV(北城天地店)', 2, 'https://p1.meituan.net/merchantpic/598c83a8c0d06fe79ca01056e214d345875600.jpg,https://qcloud.dpfile.com/pc/HhvI0YyocYHRfGwJWqPQr34hRGRl4cWdvlNwn3dqghvi4WXlM2FY1te0-7pE3Wb9_Gd2X_f-v9T8Yj4uLt25Gg.jpg,https://qcloud.dpfile.com/pc/F5ZVzZaXFE27kvQzPnaL4V8O9QCpVw2nkzGrxZE8BqXgkfyTpNExfNG5CEPQX4pjGybIjx5eX6WNgCPvcASYAw.jpg', 'D32天阳购物中心', '湖州街567号北城天地5层', 120.130453, 30.327655, 58, 0000018997, 0000001857, 41, '12:00-02:00', '2021-12-22 20:38:54', '2021-12-22 20:40:04');
INSERT INTO `tb_shop` VALUES (14, '星聚会KTV(拱墅区万达店)', 2, 'https://p0.meituan.net/dpmerchantpic/f4cd6d8d4eb1959c3ea826aa05a552c01840451.jpg,https://p0.meituan.net/dpmerchantpic/2efc07aed856a8ab0fc75c86f4b9b0061655777.jpg,https://qcloud.dpfile.com/pc/zWfzzIorCohKT0bFwsfAlHuayWjI6DBEMPHHncmz36EEMU9f48PuD9VxLLDAjdoU_Gd2X_f-v9T8Yj4uLt25Gg.jpg', '北部新城', '杭行路666号万达广场C座1-2F', 120.128958, 30.337252, 60, 0000017771, 0000000685, 47, '10:00-22:00', '2021-12-22 20:48:54', '2021-12-22 20:48:54');
INSERT INTO `tb_shop` VALUES (15, '绿茶餐厅(远洋乐堤港店)', 1, 'https://img.meituan.net/msmerchant/232f8fdf09050838bd33fb24e79f30f9606056.jpg,https://qcloud.dpfile.com/pc/rDe48Xe15nQOHCcEEkmKUp5wEKWbimt-HDeqYRWsYJseXNncvMiXbuED7x1tXqN4uzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg', '拱宸桥/上塘', '丽水路58号远洋乐堤港B区3层', 120.1503, 30.31245, 95, 0000009876, 0000006543, 48, '10:30-21:30', '2022-01-05 11:00:00', '2022-01-05 11:00:00');
INSERT INTO `tb_shop` VALUES (16, '外婆家(水晶城购物中心店)', 1, 'https://img.meituan.net/msmerchant/054b5de0ba0b50c18a620cc37482129a45739.jpg,https://img.meituan.net/msmerchant/59b7eff9b60908d52bd4aea9ff356e6d145920.jpg', '大关', '上塘路458号水晶城购物中心F5', 120.1582, 30.311, 72, 0000007654, 0000005210, 45, '10:30-21:00', '2022-01-05 11:30:00', '2022-01-05 11:30:00');
INSERT INTO `tb_shop` VALUES (17, '老头儿油爆虾(运河上街店)', 1, 'https://p0.meituan.net/biztone/694233_1619500156517.jpeg,https://img.meituan.net/msmerchant/876ca8983f7395556eda9ceb064e6bc51840883.png', '运河上街', '台州路2号运河上街购物中心F4', 120.151, 30.3256, 110, 0000005432, 0000003210, 46, '10:30-21:30', '2022-01-05 12:00:00', '2022-01-05 12:00:00');
INSERT INTO `tb_shop` VALUES (18, '知味观(湖墅南路店)', 1, 'https://p0.meituan.net/bbia/c1870d570e73accbc9fee90b48faca41195272.jpg,https://img.meituan.net/msmerchant/0b463f545355c8d8f021eb2987dcd0c8567811.jpg', '大关', '湖墅南路349号', 120.145, 30.306, 60, 0000003987, 0000001876, 43, '07:00-21:00', '2022-01-05 12:30:00', '2022-01-05 12:30:00');
INSERT INTO `tb_shop` VALUES (19, '丝雨(环城西路店)', 3, 'https://p0.meituan.net/dpmerchantpic/63833f6ba0393e2e8722420ef33f3d40466664.jpg,https://p0.meituan.net/dpmerchantpic/ae3c94cc92c529c4b1d7f68cebed33fa105810.png', '武林', '环城西路与体育场路交汇处东北', 120.1553, 30.2765, 133, 0000002567, 0000001234, 48, '10:00-22:00', '2022-01-06 10:00:00', '2022-01-06 10:00:00');
INSERT INTO `tb_shop` VALUES (20, '男朋友的理髪馆(Boyfriend Barber Shop)', 3, 'https://p0.meituan.net/dpmerchantpic/53e74b200211d68988a4f02ae9912c6c1076826.jpg,https://qcloud.dpfile.com/pc/4iWtIvzLzwM2MGgyPu1PCDb4SWEaKqUeHm--YAt1EwR5tn8kypBcqNwHnjg96EvT_Gd2X_f-v9T8Yj4uLt25Gg.jpg', '武林', '武林路446号', 120.1592, 30.2788, 128, 0000001987, 000000987, 47, '10:00-22:00', '2022-01-06 10:30:00', '2022-01-06 10:30:00');
INSERT INTO `tb_shop` VALUES (21, '逸丝时尚专业造型烫染基地', 3, 'https://img.meituan.net/msmerchant/cf3dff697bf7f6e11f4b79c4e7d989e4591290.jpg,https://p0.meituan.net/joymerchant/a575fd4adb0b9099c5c410058148b307-674435191.jpg', '北部新城', '新文路3号', 120.1518, 30.352, 88, 0000001678, 000000876, 44, '09:00-21:00', '2022-01-06 11:00:00', '2022-01-06 11:00:00');
INSERT INTO `tb_shop` VALUES (22, '乐刻运动健身(运河上街店)', 4, 'https://p0.meituan.net/dpmerchantpic/f4cd6d8d4eb1959c3ea826aa05a552c01840451.jpg,https://qcloud.dpfile.com/pc/zWfzzIorCohKT0bFwsfAlHuayWjI6DBEMPHHncmz36EEMU9f48PuD9VxLLDAjdoU_Gd2X_f-v9T8Yj4uLt25Gg.jpg', '运河上街', '衢州路2-38号', 120.1495, 30.3245, 99, 0000003456, 0000002109, 47, '00:00-24:00', '2022-01-06 13:00:00', '2022-01-06 13:00:00');
INSERT INTO `tb_shop` VALUES (23, '乐刻运动(万通中心店)', 4, 'https://p0.meituan.net/dpmerchantpic/2efc07aed856a8ab0fc75c86f4b9b0061655777.jpg,https://qcloud.dpfile.com/pc/HhvI0YyocYHRfGwJWqPQr34hRGRl4cWdvlNwn3dqghvi4WXlM2FY1te0-7pE3Wb9_Gd2X_f-v9T8Yj4uLt25Gg.jpg', '大关', '大关路189号万通中心E座', 120.143, 30.318, 99, 0000002987, 0000001876, 45, '00:00-24:00', '2022-01-06 13:30:00', '2022-01-06 13:30:00');
INSERT INTO `tb_shop` VALUES (24, '乐刻健身(太阳城店)', 4, 'https://p1.meituan.net/merchantpic/598c83a8c0d06fe79ca01056e214d345875600.jpg,https://qcloud.dpfile.com/pc/F5ZVzZaXFE27kvQzPnaL4V8O9QCpVw2nkzGrxZE8BqXgkfyTpNExfNG5CEPQX4pjGybIjx5eX6WNgCPvcASYAw.jpg', '北部新城', '石祥路249号太阳城4层', 120.139, 30.349, 99, 0000002134, 0000001098, 44, '09:00-22:00', '2022-01-06 14:00:00', '2022-01-06 14:00:00');
INSERT INTO `tb_shop` VALUES (25, '臻本足道', 5, 'https://qcloud.dpfile.com/pc/jiclIsCKmOI2arxKN1Uf0Hx3PucIJH8q0QSz-Z8llzcN56-_QiKuOvyio1OOxsRtFoXqu0G3iT2T27qat3WhLVEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vfCF2ubeXzk49OsGrXt_KYDCngOyCwZK-s3fqawWswzk.jpg,https://img.meituan.net/msmerchant/e71a2d0d693b3033c15522c43e03f09198239.jpg', '武林', '密渡桥路98号', 120.1496, 30.2905, 139, 0000004567, 0000002345, 46, '11:00-02:00', '2022-01-06 15:00:00', '2022-01-06 15:00:00');
INSERT INTO `tb_shop` VALUES (26, '良子足浴(三立开元店)', 5, 'https://img.meituan.net/msmerchant/9f8a966d60ffba00daf35458522273ca658239.jpg,https://img.meituan.net/msmerchant/ef9ca5ef6c05d381946fe4a9aa7d9808554502.jpg', '大关', '绍兴路538号浙江三立开元名都大酒店4层', 120.168, 30.333, 158, 0000005678, 0000003210, 48, '11:00-02:00', '2022-01-06 15:30:00', '2022-01-06 15:30:00');
INSERT INTO `tb_shop` VALUES (27, '秦桥足道', 5, 'https://qcloud.dpfile.com/pc/MZTdRDqCZdbPDUO0Hk6lZENRKzpKRF7kavrkEI99OxqBZTzPfIxa5E33gBfGouhFuzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg,https://p0.meituan.net/mogu/397e40c28fc87715b3d5435710a9f88d706914.jpg', '北部新城', '丰庆路492号龙禾商务中心1幢4层', 120.108, 30.334, 128, 0000003456, 0000001987, 44, '10:00-01:00', '2022-01-06 16:00:00', '2022-01-06 16:00:00');
INSERT INTO `tb_shop` VALUES (28, '善若水足道', 5, 'https://img.meituan.net/msmerchant/909434939a49b36f340523232924402166854.jpg,https://img.meituan.net/msmerchant/32fd2425f12e27db0160e837461c10303700032.jpg', '武林', '中山北路315号', 120.1665, 30.2785, 119, 0000002345, 0000001234, 45, '11:00-01:00', '2022-01-06 16:30:00', '2022-01-06 16:30:00');
INSERT INTO `tb_shop` VALUES (29, '思妍丽(华浙广场店)', 6, 'https://p0.meituan.net/biztone/163160492_1624251899456.jpeg,https://img.meituan.net/msmerchant/e478eb16f7e31a7f8b29b5e3bab6de205500837.jpg', '武林', '华浙广场9号', 120.156, 30.289, 398, 0000004567, 0000003210, 49, '10:00-22:00', '2022-01-07 10:00:00', '2022-01-07 10:00:00');
INSERT INTO `tb_shop` VALUES (30, '禾颜社(蓝天店)', 6, 'https://img.meituan.net/msmerchant/6173eb1d18b9d70ace7fdb3f2dd939662884857.jpg,https://img.meituan.net/msmerchant/c3c2516939efaf36c4ccc64b0e629fad587907.jpg', '武林', '莫干山路24号蓝天商务中心2楼', 120.1428, 30.295, 168, 0000001987, 0000001098, 46, '10:00-21:00', '2022-01-07 10:30:00', '2022-01-07 10:30:00');
INSERT INTO `tb_shop` VALUES (31, 'EC连锁·泰式·SPA(大关店)', 6, 'https://qcloud.dpfile.com/pc/rDe48Xe15nQOHCcEEkmKUp5wEKWbimt-HDeqYRWsYJseXNncvMiXbuED7x1tXqN4uzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg,https://img.meituan.net/msmerchant/054b5de0ba0b50c18a620cc37482129a45739.jpg', '大关', '上塘街道新青年广场', 120.152, 30.318, 188, 0000001543, 000000876, 47, '10:00-23:00', '2022-01-07 11:00:00', '2022-01-07 11:00:00');
INSERT INTO `tb_shop` VALUES (32, 'MELAND CLUB(城西银泰旗舰店)', 7, 'https://p0.meituan.net/dpmerchantpic/f4cd6d8d4eb1959c3ea826aa05a552c01840451.jpg,https://img.meituan.net/msmerchant/232f8fdf09050838bd33fb24e79f30f9606056.jpg,https://qcloud.dpfile.com/pc/zWfzzIorCohKT0bFwsfAlHuayWjI6DBEMPHHncmz36EEMU9f48PuD9VxLLDAjdoU_Gd2X_f-v9T8Yj4uLt25Gg.jpg', '城西银泰', '萍水街丰潭路380号城西银泰城L2', 120.106, 30.2893, 288, 0000006789, 0000004567, 49, '10:00-21:00', '2022-01-07 13:00:00', '2022-01-07 13:00:00');
INSERT INTO `tb_shop` VALUES (33, 'MELAND CLUB(城北万象城店)', 7, 'https://qcloud.dpfile.com/pc/HhvI0YyocYHRfGwJWqPQr34hRGRl4cWdvlNwn3dqghvi4WXlM2FY1te0-7pE3Wb9_Gd2X_f-v9T8Yj4uLt25Gg.jpg,https://p0.meituan.net/dpmerchantpic/2efc07aed856a8ab0fc75c86f4b9b0061655777.jpg', '北部新城', '杭行路1499号城北万象城L3', 120.121, 30.351, 166, 0000005432, 0000003210, 48, '10:00-21:00', '2022-01-07 13:30:00', '2022-01-07 13:30:00');
INSERT INTO `tb_shop` VALUES (34, 'Helens海伦司小酒馆(运河商厦店)', 8, 'https://p0.meituan.net/joymerchant/a575fd4adb0b9099c5c410058148b307-674435191.jpg,https://p0.meituan.net/merchantpic/68f11bf850e25e437c5f67decfd694ab2541634.jpg', '运河商厦', '湖墅南路488号运河商厦', 120.1443, 30.3095, 84, 0000003210, 0000001987, 44, '19:00-02:00', '2022-01-07 14:00:00', '2022-01-07 14:00:00');
INSERT INTO `tb_shop` VALUES (35, '一品脱酒馆(远洋乐堤港店)', 8, 'https://p0.meituan.net/dpmerchantpic/53e74b200211d68988a4f02ae9912c6c1076826.jpg,https://qcloud.dpfile.com/pc/4iWtIvzLzwM2MGgyPu1PCDb4SWEaKqUeHm--YAt1EwR5tn8kypBcqNwHnjg96EvT_Gd2X_f-v9T8Yj4uLt25Gg.jpg', '远洋乐堤港', '丽水路58号远洋乐堤港国际Yeah街区C112', 120.1506, 30.3129, 112, 0000002109, 0000001234, 45, '18:00-02:00', '2022-01-07 14:30:00', '2022-01-07 14:30:00');
INSERT INTO `tb_shop` VALUES (36, 'MILL(乐堤港店)', 8, 'https://qcloud.dpfile.com/pc/WZsJWRI447x1VG2x48Ujgu7vwqksi_9WitdKI4j3jvIgX4MZOpGNaFtM93oSSizbGybIjx5eX6WNgCPvcASYAw.jpg,https://img.meituan.net/msmerchant/59b7eff9b60908d52bd4aea9ff356e6d145920.jpg', '远洋乐堤港', '丽水路远洋乐堤港B区L1-B108', 120.1492, 30.3116, 127, 0000001876, 000000987, 46, '18:00-02:00', '2022-01-07 15:00:00', '2022-01-07 15:00:00');
INSERT INTO `tb_shop` VALUES (37, '胡桃里音乐酒馆(凯兴俪座店)', 8, 'https://img.meituan.net/msmerchant/e71a2d0d693b3033c15522c43e03f09198239.jpg,https://p0.meituan.net/biztone/163160492_1624251899456.jpeg', '北部新城', '东文街89号凯兴俪座1层', 120.152, 30.348, 150, 0000004321, 0000002987, 47, '11:00-02:00', '2022-01-07 15:30:00', '2022-01-07 15:30:00');
INSERT INTO `tb_shop` VALUES (38, '大隐音乐酒吧(万达商业中心店)', 8, 'https://p0.meituan.net/dpmerchantpic/63833f6ba0393e2e8722420ef33f3d40466664.jpg,https://p0.meituan.net/dpmerchantpic/ae3c94cc92c529c4b1d7f68cebed33fa105810.png', '北部新城', '杭行路666号万达商业中心F1', 120.1253, 30.3372, 112, 0000002987, 0000001654, 45, '18:00-02:00', '2022-01-07 16:00:00', '2022-01-07 16:00:00');
INSERT INTO `tb_shop` VALUES (39, '初样轰趴', 9, 'https://img.meituan.net/msmerchant/9f8a966d60ffba00daf35458522273ca658239.jpg,https://img.meituan.net/msmerchant/ef9ca5ef6c05d381946fe4a9aa7d9808554502.jpg', '北部新城', '莫干山路蓝钻天成2幢2单元2102室', 120.115, 30.333, 150, 0000001543, 000000876, 45, '00:00-24:00', '2022-01-08 10:00:00', '2022-01-08 10:00:00');
INSERT INTO `tb_shop` VALUES (40, '蒸汽轰趴馆(云天财富大厦店)', 9, 'https://qcloud.dpfile.com/pc/MZTdRDqCZdbPDUO0Hk6lZENRKzpKRF7kavrkEI99OxqBZTzPfIxa5E33gBfGouhFuzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg,https://p0.meituan.net/bbia/c1870d570e73accbc9fee90b48faca41195272.jpg', '运河文化广场', '环城北路69号', 120.17, 30.2835, 120, 000000987, 000000543, 43, '10:00-02:00', '2022-01-08 10:30:00', '2022-01-08 10:30:00');
INSERT INTO `tb_shop` VALUES (41, '漆橙车间(城北万达店)', 9, 'https://p0.meituan.net/dpmerchantpic/f4cd6d8d4eb1959c3ea826aa05a552c01840451.jpg,https://qcloud.dpfile.com/pc/zWfzzIorCohKT0bFwsfAlHuayWjI6DBEMPHHncmz36EEMU9f48PuD9VxLLDAjdoU_Gd2X_f-v9T8Yj4uLt25Gg.jpg', '北部新城', '杭行路666号拱墅万达广场', 120.124, 30.3378, 98, 0000002109, 0000001234, 46, '00:00-24:00', '2022-01-08 11:00:00', '2022-01-08 11:00:00');
INSERT INTO `tb_shop` VALUES (42, 'Asm美甲美睫(万达广场店)', 10, 'https://p0.meituan.net/joymerchant/a575fd4adb0b9099c5c410058148b307-674435191.jpg,https://p0.meituan.net/merchantpic/68f11bf850e25e437c5f67decfd694ab2541634.jpg', '北部新城', '杭行路666号拱墅万达广场4幢2单元142号', 120.1258, 30.3375, 99, 0000001345, 000000765, 45, '10:00-22:00', '2022-01-08 11:30:00', '2022-01-08 11:30:00');
INSERT INTO `tb_shop` VALUES (43, 'X·Y美甲美睫', 10, 'https://img.meituan.net/msmerchant/cf3dff697bf7f6e11f4b79c4e7d989e4591290.jpg,https://img.meituan.net/msmerchant/0b463f545355c8d8f021eb2987dcd0c8567811.jpg', '拱宸桥/上塘', '拱宸桥街道风景街318号', 120.148, 30.322, 211, 0000001098, 000000654, 47, '10:00-21:30', '2022-01-08 12:00:00', '2022-01-08 12:00:00');
INSERT INTO `tb_shop` VALUES (44, '小黑瓶美甲美睫(西湖文化广场店)', 10, 'https://qcloud.dpfile.com/pc/jiclIsCKmOI2arxKN1Uf0Hx3PucIJH8q0QSz-Z8llzcN56-_QiKuOvyio1OOxsRtFoXqu0G3iT2T27qat3WhLVEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vfCF2ubeXzk49OsGrXt_KYDCngOyCwZK-s3fqawWswzk.jpg,https://p0.meituan.net/mogu/397e40c28fc87715b3d5435710a9f88d706914.jpg', '武林', '西湖文化广场地铁站B口', 120.1653, 30.2857, 150, 000000876, 000000432, 46, '10:00-21:30', '2022-01-08 12:30:00', '2022-01-08 12:30:00');

-- 初始数据未包含 city 列值，统一回填（初始数据均为杭州店铺）
UPDATE `tb_shop` SET `city` = '杭州' WHERE `city` = '';

-- ----------------------------
-- Table structure for tb_shop_type
-- ----------------------------
DROP TABLE IF EXISTS `tb_shop_type`;
CREATE TABLE `tb_shop_type`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型名称',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图标',
  `sort` int(3) UNSIGNED NULL DEFAULT NULL COMMENT '顺序',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_shop_type
-- ----------------------------
INSERT INTO `tb_shop_type` VALUES (1, '美食', '/types/ms.png', 1, '2021-12-22 20:17:47', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (2, 'KTV', '/types/KTV.png', 2, '2021-12-22 20:18:27', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (3, '丽人·美发', '/types/lrmf.png', 3, '2021-12-22 20:18:48', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (4, '健身运动', '/types/jsyd.png', 10, '2021-12-22 20:19:04', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (5, '按摩·足疗', '/types/amzl.png', 5, '2021-12-22 20:19:27', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (6, '美容SPA', '/types/spa.png', 6, '2021-12-22 20:19:35', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (7, '亲子游乐', '/types/qzyl.png', 7, '2021-12-22 20:19:53', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (8, '酒吧', '/types/jiuba.png', 8, '2021-12-22 20:20:02', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (9, '轰趴馆', '/types/hpg.png', 9, '2021-12-22 20:20:08', '2021-12-23 11:24:31');
INSERT INTO `tb_shop_type` VALUES (10, '美睫·美甲', '/types/mjmj.png', 4, '2021-12-22 20:21:46', '2021-12-23 11:24:31');

-- ----------------------------
-- Table structure for tb_user
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '手机号码',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '密码，加密存储',
  `nick_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '昵称，默认是用户id',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '人物头像',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniqe_key_phone`(`phone`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1010 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_user
-- ----------------------------
INSERT INTO `tb_user` VALUES (1, '13686869696', '', '小鱼同学', '/imgs/blogs/blog1.jpg', '2021-12-24 10:27:19', '2022-01-11 16:04:00');
INSERT INTO `tb_user` VALUES (2, '13838411438', '', '可可今天不吃肉', '/imgs/icons/kkjtbcr.jpg', '2021-12-24 15:14:39', '2021-12-28 19:58:04');
INSERT INTO `tb_user` VALUES (3, '13705710001', '', '食神老王', '/imgs/blogs/blog1.jpg', '2021-12-25 10:27:19', '2022-01-05 10:00:00');
INSERT INTO `tb_user` VALUES (4, '13456789011', '', 'user_slxaxy2au9f3tanffaxr', '', '2022-01-07 12:07:53', '2022-01-07 12:07:53');
INSERT INTO `tb_user` VALUES (5, '13456789001', '', 'user_n0bb8mwwg4', '', '2022-01-07 16:11:33', '2022-01-07 16:11:33');
INSERT INTO `tb_user` VALUES (6, '13705710002', '', '柠檬不酸', '/imgs/icons/kkjtbcr.jpg', '2022-01-08 12:00:00', '2022-01-08 12:00:00');
INSERT INTO `tb_user` VALUES (7, '13705710003', '', '摄影师小北', '/imgs/blogs/blog1.jpg', '2022-01-09 09:30:00', '2022-01-09 09:30:00');
INSERT INTO `tb_user` VALUES (8, '13705710004', '', '糖糖爱遛娃', '/imgs/blogs/blog1.jpg', '2022-01-10 14:00:00', '2022-01-10 14:00:00');
INSERT INTO `tb_user` VALUES (9, '13705710005', '', '夜猫子阿杰', '/imgs/blogs/blog1.jpg', '2022-01-11 20:00:00', '2022-01-11 20:00:00');
INSERT INTO `tb_user` VALUES (10, '13705710006', '', '健身狂人', '/imgs/blogs/blog1.jpg', '2022-01-12 08:00:00', '2022-01-12 08:00:00');

-- ----------------------------
-- Table structure for tb_user_info
-- ----------------------------
DROP TABLE IF EXISTS `tb_user_info`;
CREATE TABLE `tb_user_info`  (
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '主键，用户id',
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '城市名称',
  `introduce` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '个人介绍，不要超过128个字符',
  `fans` int(8) UNSIGNED NULL DEFAULT 0 COMMENT '粉丝数量',
  `followee` int(8) UNSIGNED NULL DEFAULT 0 COMMENT '关注的人的数量',
  `gender` tinyint(1) UNSIGNED NULL DEFAULT 0 COMMENT '性别，0：男，1：女',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  `credits` int(8) UNSIGNED NULL DEFAULT 0 COMMENT '积分',
  `level` tinyint(1) UNSIGNED NULL DEFAULT 0 COMMENT '会员级别，0~9级,0代表未开通会员',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_user_info
-- ----------------------------
INSERT INTO `tb_user_info` VALUES (1, '杭州', '美食探店达人，带你吃遍杭州的大街小巷', 326, 128, 0, '1995-06-18', 1280, 1, '2021-12-24 10:27:19', '2022-01-11 16:04:00');
INSERT INTO `tb_user_info` VALUES (2, '杭州', '一个不想长胖的吃货，甜品是我的本命', 208, 86, 1, '1998-03-08', 960, 1, '2021-12-24 15:14:39', '2021-12-28 19:58:04');
INSERT INTO `tb_user_info` VALUES (3, '杭州', '人间烟火气，最抚凡人心', 156, 65, 0, '1993-11-20', 720, 1, '2021-12-25 10:27:19', '2022-01-05 10:00:00');
INSERT INTO `tb_user_info` VALUES (4, '杭州', '', 12, 5, 0, NULL, 60, 0, '2022-01-07 12:07:53', '2022-01-07 12:07:53');
INSERT INTO `tb_user_info` VALUES (5, '杭州', '', 8, 3, 0, NULL, 30, 0, '2022-01-07 16:11:33', '2022-01-07 16:11:33');
INSERT INTO `tb_user_info` VALUES (6, '杭州', '咖啡续命，甜品治愈', 89, 42, 1, '1997-05-15', 350, 1, '2022-01-08 12:00:00', '2022-01-08 12:00:00');
INSERT INTO `tb_user_info` VALUES (7, '杭州', '用镜头记录城市的美好瞬间', 512, 180, 0, '1992-09-30', 1500, 1, '2022-01-09 09:30:00', '2022-01-09 09:30:00');
INSERT INTO `tb_user_info` VALUES (8, '杭州', '两个娃的妈，分享遛娃好去处', 234, 98, 1, '1990-01-01', 880, 1, '2022-01-10 14:00:00', '2022-01-10 14:00:00');
INSERT INTO `tb_user_info` VALUES (9, '杭州', '深夜的酒，清晨的粥', 176, 72, 0, '1996-07-07', 640, 1, '2022-01-11 20:00:00', '2022-01-11 20:00:00');
INSERT INTO `tb_user_info` VALUES (10, '杭州', '自律给我自由', 198, 54, 0, '1994-04-04', 1020, 1, '2022-01-12 08:00:00', '2022-01-12 08:00:00');

-- ----------------------------
-- Table structure for tb_voucher
-- ----------------------------
DROP TABLE IF EXISTS `tb_voucher`;
CREATE TABLE `tb_voucher`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '商铺id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '代金券标题',
  `sub_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '副标题',
  `rules` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '使用规则',
  `pay_value` bigint(10) UNSIGNED NOT NULL COMMENT '支付金额，单位是分。例如200代表2元',
  `actual_value` bigint(10) NOT NULL COMMENT '抵扣金额，单位是分。例如200代表2元',
  `type` tinyint(1) UNSIGNED NOT NULL DEFAULT 0 COMMENT '0,普通券；1,秒杀券',
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 1 COMMENT '1,上架; 2,下架; 3,过期',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_voucher
-- ----------------------------
INSERT INTO `tb_voucher` VALUES (1, 1, '50元代金券', '周一至周日均可使用', '全场通用\\n无需预约\\n可无限叠加\\不兑现、不找零\\n仅限堂食', 4750, 5000, 0, 1, '2022-01-04 09:42:39', '2022-01-04 09:43:31');
INSERT INTO `tb_voucher` VALUES (2, 1, '100元代金券', '周一至周日均可使用', '全场通用\\n无需预约\\n可无限叠加\\不兑现、不找零\\n仅限堂食', 9500, 10000, 0, 1, '2022-01-05 10:00:00', '2022-01-05 10:00:00');
INSERT INTO `tb_voucher` VALUES (3, 2, '5元代金券', '节假日通用', '全场通用\\n每人限购1张\\n有效期至年底\\n不兑现、不找零', 100, 500, 1, 1, '2022-01-05 10:00:00', '2022-01-05 10:00:00');
INSERT INTO `tb_voucher` VALUES (4, 1, '1元秒杀50元代金券', '仅限工作日使用', '每人限购1张\\n仅限堂食\\n不兑现、不找零', 100, 5000, 1, 1, '2022-01-05 10:00:00', '2022-01-05 10:00:00');
INSERT INTO `tb_voucher` VALUES (5, 15, '100元代金券', '乐堤港店通用', '全场通用\\n无需预约\\n可无限叠加\\n仅限堂食', 9500, 10000, 0, 1, '2022-01-05 11:00:00', '2022-01-05 11:00:00');
INSERT INTO `tb_voucher` VALUES (6, 34, '9.9元购50元酒水券', '周一至周四可用', '每人限购2张\\n仅限酒水\\n不兑现、不找零', 990, 5000, 1, 1, '2022-01-07 14:00:00', '2022-01-07 14:00:00');
INSERT INTO `tb_voucher` VALUES (7, 22, '200元代金券', '年卡专享优惠', '仅限年卡用户\\n不与其他优惠叠加', 19000, 20000, 0, 1, '2022-01-06 13:00:00', '2022-01-06 13:00:00');
INSERT INTO `tb_voucher` VALUES (8, 32, '198元亲子票立减100', '工作日通用', '每人限购1张\\n需提前预约\\n法定节假日不可用', 9900, 19800, 1, 1, '2022-01-07 13:00:00', '2022-01-07 13:00:00');
INSERT INTO `tb_voucher` VALUES (9, 3, '88元代金券', '运河上街店通用', '全场通用\\n无需预约\\n可无限叠加\\n仅限堂食', 8300, 8800, 0, 1, '2022-01-05 12:00:00', '2022-01-05 12:00:00');

-- ----------------------------
-- Table structure for tb_voucher_order
-- ----------------------------
DROP TABLE IF EXISTS `tb_voucher_order`;
CREATE TABLE `tb_voucher_order`  (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '下单的用户id',
  `voucher_id` bigint(20) UNSIGNED NOT NULL COMMENT '购买的代金券id',
  `pay_type` tinyint(1) UNSIGNED NOT NULL DEFAULT 1 COMMENT '支付方式 1：余额支付；2：支付宝；3：微信',
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 1 COMMENT '订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `pay_time` timestamp NULL DEFAULT NULL COMMENT '支付时间',
  `use_time` timestamp NULL DEFAULT NULL COMMENT '核销时间',
  `refund_time` timestamp NULL DEFAULT NULL COMMENT '退款时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_voucher` (`user_id`, `voucher_id`) USING BTREE,
  INDEX `idx_user_time`(`user_id`, `create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_voucher_order
-- ----------------------------
INSERT INTO `tb_voucher_order` VALUES (1, 1, 1, 1, 3, '2022-01-05 12:00:00', '2022-01-05 12:10:00', '2022-01-06 18:30:00', NULL, '2022-01-06 18:30:00');
INSERT INTO `tb_voucher_order` VALUES (2, 2, 3, 1, 2, '2022-01-05 15:00:00', '2022-01-05 15:05:00', NULL, NULL, '2022-01-05 15:05:00');
INSERT INTO `tb_voucher_order` VALUES (3, 3, 4, 1, 1, '2022-01-06 10:00:00', NULL, NULL, NULL, '2022-01-06 10:00:00');
INSERT INTO `tb_voucher_order` VALUES (4, 6, 6, 1, 2, '2022-01-08 20:00:00', '2022-01-08 20:10:00', NULL, NULL, '2022-01-08 20:10:00');
INSERT INTO `tb_voucher_order` VALUES (5, 8, 8, 2, 1, '2022-01-10 09:00:00', NULL, NULL, NULL, '2022-01-10 09:00:00');
INSERT INTO `tb_voucher_order` VALUES (6, 1, 2, 1, 2, '2022-01-06 11:00:00', '2022-01-06 11:05:00', NULL, NULL, '2022-01-06 11:05:00');

SET FOREIGN_KEY_CHECKS = 1;
