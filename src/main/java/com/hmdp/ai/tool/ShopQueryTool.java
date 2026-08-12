package com.hmdp.ai.tool;

import com.hmdp.ai.tool.vo.ShopVO;
import com.hmdp.ai.tool.vo.VoucherVO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 业务查询工具（Function Calling）：
 * 模型在回答店铺推荐、优惠券等问题时自动调用，数据来源于 hmdp 业务库（只读）。
 *
 * <p>注意：仅做只读查询，不涉及任何写操作，与主项目业务完全解耦。
 */
@Component
public class ShopQueryTool {

    private static final String SHOP_SQL = """
            SELECT s.id, s.name, t.name AS type_name, s.area, s.address,
                   s.avg_price, s.sold, s.comments, s.score, s.open_hours
            FROM tb_shop s
            LEFT JOIN tb_shop_type t ON s.type_id = t.id
            WHERE s.name LIKE CONCAT('%', ?, '%')
               OR t.name LIKE CONCAT('%', ?, '%')
               OR s.area LIKE CONCAT('%', ?, '%')
            ORDER BY s.score DESC, s.comments DESC
            LIMIT 10
            """;

    private static final String VOUCHER_SQL = """
            SELECT id, shop_id, title, sub_title, pay_value, actual_value, type, status
            FROM tb_voucher
            WHERE shop_id = ? AND status = 1
            LIMIT 20
            """;

    private final JdbcTemplate jdbcTemplate;

    public ShopQueryTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按关键词查询店铺：支持店铺类型（如：美食、KTV）、店铺名称、商圈（如：大关、运河上街）
     */
    @Tool(description = "查询店铺列表，支持店铺类型名称（如美食、KTV）、店铺名或商圈关键词，返回评分最高的前10家")
    public List<ShopVO> queryShops(
            @ToolParam(description = "查询关键词，可以是店铺类型名称、店铺名或商圈，例如：美食、KTV、103茶餐厅、运河上街") String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        String like = keyword.trim();
        return jdbcTemplate.query(SHOP_SQL, (rs, rowNum) -> new ShopVO(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("type_name"),
                rs.getString("area"),
                rs.getString("address"),
                rs.getInt("avg_price"),
                rs.getInt("sold"),
                rs.getInt("comments"),
                rs.getDouble("score") / 10.0,
                rs.getString("open_hours")
        ), like, like, like);
    }

    /**
     * 查询指定店铺正在上架的优惠券/代金券
     */
    @Tool(description = "查询指定店铺当前上架的优惠券列表，返回券标题、支付金额与抵扣金额（单位：元）")
    public List<VoucherVO> queryVouchers(
            @ToolParam(description = "店铺ID，通常来自 queryShops 的返回结果") Long shopId) {
        if (shopId == null || shopId <= 0) {
            return Collections.emptyList();
        }
        return jdbcTemplate.query(VOUCHER_SQL, (rs, rowNum) -> new VoucherVO(
                rs.getLong("id"),
                rs.getLong("shop_id"),
                rs.getString("title"),
                rs.getString("sub_title"),
                rs.getDouble("pay_value") / 100.0,
                rs.getDouble("actual_value") / 100.0,
                rs.getInt("type"),
                rs.getInt("status")
        ), shopId);
    }
}
