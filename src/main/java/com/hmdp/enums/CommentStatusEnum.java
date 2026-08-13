package com.hmdp.enums;

/**
 * 评论状态枚举：与 tb_blog_comments.status 对应（0 正常、1 被举报、2 禁止查看）。
 *
 * <p>注意：该字段为三态语义，实体中应使用 Integer 而非 Boolean 存储，本枚举仅作状态码字典。
 */
public enum CommentStatusEnum {

    NORMAL(0, "正常"),
    REPORTED(1, "被举报"),
    FORBIDDEN(2, "禁止查看");

    private final int code;

    private final String desc;

    CommentStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
