package com.current.rfyy.constant;

import java.math.BigDecimal;

/**
 * @Author: zzy
 * @Date: 2026/1/8 15:19
 * @Description: TODO 常量
 **/
public class RfyyConstant {

    // 金额阈值
    public static final BigDecimal AMOUNT_THRESHOLD = new BigDecimal("0.0");

    // 金额尾差
    public static final BigDecimal AMOUNT_DIFF = new BigDecimal("5.0");

    /**
     * FORCE_MATCH 最大候选采购单数，超过直接放弃
     */
    public static final Integer MAX_CANDIDATE_SIZE = 100;

    /**
     *  匹配允许最大日期隔间
     */
    public static final Integer MAX_DATE_GAP = 50;


    /**
     * 尾差匹配
     */
    public static final String WC_MATCH = "1";
    /**
     * 不进行尾差匹配
     */
    public static final String NO_WC_MATCH = "0";
}
