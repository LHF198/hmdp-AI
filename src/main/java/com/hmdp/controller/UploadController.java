package com.hmdp.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("upload")
public class UploadController {

    /**
     * 图片上传根目录（配置项 app.upload-dir，默认 nginx 静态根 frontend/html/hmdp/imgs/）
     */
    @Value("${app.upload-dir:frontend/html/hmdp/imgs/}")
    private String uploadDir;

    /**
     * 允许的图片扩展名白名单（防任意文件上传 / 存储型 XSS）
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            // 获取原始文件名称
            String originalFilename = image.getOriginalFilename();
            // 扩展名白名单校验（原始文件名可能为 null 或无后缀）
            String suffix = StrUtil.subAfter(originalFilename == null ? "" : originalFilename, ".", true)
                    .toLowerCase();
            if (StrUtil.isBlank(suffix) || !ALLOWED_EXTENSIONS.contains(suffix)) {
                return Result.fail("仅支持 jpg/jpeg/png/webp/gif 图片上传");
            }
            // 生成新文件名
            String fileName = createNewFileName(suffix);
            // 保存文件
            image.transferTo(new File(uploadDir, fileName));
            // 返回结果
            log.debug("文件上传成功，{}", fileName);
            return Result.ok(fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 删除笔记图片：POST/DELETE 语义（原 GET 带副作用且可被预加载/爬虫触发），
     * 并对文件名做路径规范化校验，拒绝 .. 穿越上传根目录
     */
    @DeleteMapping("/blog/delete")
    public Result deleteBlogImg(@RequestParam("name") String filename) {
        if (StrUtil.isBlank(filename)) {
            return Result.fail("文件名称不能为空");
        }
        // 兼容 URL 空间路径（/imgs/blogs/...）与存储相对路径（blogs/...），并去掉开头的 /
        String relative = StrUtil.removePrefix(StrUtil.removePrefix(filename, "/imgs/"), "/");
        if (StrUtil.isBlank(relative)) {
            return Result.fail("非法的文件名称");
        }
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            // 路径穿越：目标位于上传根目录之外
            return Result.fail("非法的文件名称");
        }
        File file = target.toFile();
        if (!file.exists() || file.isDirectory()) {
            return Result.fail("错误的文件名称");
        }
        FileUtil.del(file);
        return Result.ok();
    }

    /**
     * 生成按哈希分层的随机文件名（blogs/{d1}/{d2}/{uuid}.{suffix}），目录不存在时自动创建
     */
    private String createNewFileName(String suffix) {
        // 生成目录
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        // 判断目录是否存在
        File dir = new File(uploadDir, StrUtil.format("/blogs/{}/{}", d1, d2));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 生成文件名
        return StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix);
    }
}
