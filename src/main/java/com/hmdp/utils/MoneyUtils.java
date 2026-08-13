package com.hmdp.utils;

/**
 * 金额工具：数据库金额字段统一以「分」存储（最小货币单位），对外展示/接口输出统一以「元」为单位。
 */
public final class MoneyUtils {

    /**
     * 分与元的换算系数
     */
    public static final double FEN_PER_YUAN = 100.0;

    private MoneyUtils() {
    }

    /**
     * 分转元
     *
     * @param fen 金额（分）
     * @return 金额（元）
     */
    public static double fenToYuan(double fen) {
        return fen / FEN_PER_YUAN;
    }
}
