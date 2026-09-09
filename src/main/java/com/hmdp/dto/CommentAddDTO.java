package com.hmdp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布评论/回复请求体
 */
@Data
public class CommentAddDTO {

    /** 评论内容（必填，最多255字） */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 255, message = "评论内容过长（最多255字）")
    private String content;

    /** 父评论id（一级评论不传，默认为0） */
    private Long parentId;

    /** 被回复的评论id（一级评论不传，默认为0） */
    private Long answerId;
}
