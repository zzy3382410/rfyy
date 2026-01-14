package com.current.rfyy.constant;

/**
 * @Author: zzy
 * @Date: 2026/1/9 11:11
 * @Description: TODO 匹配策略枚举
 **/
public enum StrategyEnum {

    SPMC_PH_JE_SL("SPMC_PH_JE_SL", "MatchBySpmcAndPhAndJeAndSl"),
    SPMC_JE_SL_RQ("SPMC_JE_SL_RQ", "MatchBySpmcAndJeAndSlAndRq"),
    SPMC_JE_SL("SPMC_JE_SL", "MatchBySpmcAndJeAndSl"),
    PH_JE_SL("PH_JE_SL", "MatchByPhAndJeAndSl"),
    PH_RQ_JE_SL("PH_RQ_JE_SL", "MatchByPhAndRqAndJeAndSl"),
    FPMX_CGDMX_SPMC_JE_SL("FPMX_CGDMX_SPMC_JE_SL", "MathchByFpMxAndCgdMx");


    private final String strategy;
    private final String className;

    StrategyEnum(String strategy, String className) {
        this.strategy = strategy;
        this.className = className;
    }

    public String getstrategy() {
        return strategy;
    }

    public String getclassName() {
        return className;
    }
}
