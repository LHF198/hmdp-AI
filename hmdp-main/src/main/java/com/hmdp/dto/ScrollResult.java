package com.hmdp.dto;

import lombok.Data;

import java.util.List;

/**
 * 滚动分页结果：minTime 为本次拉取的最小时间戳（下次查询的游标），
 * offset 为与 minTime 同分的元素数量（用于处理分数重复场景）
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
