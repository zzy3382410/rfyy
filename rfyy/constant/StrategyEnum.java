package com.current.rfyy.constant;

/**
 * @Author: zzy
 * @Date: 2026/1/9 11:11
 * @Description: TODO 匹配策略枚举
 **/
public enum StrategyEnum {

    SPMC_PH("SPMC_PH", "MatchBySpmcAndPhStrategy"),
    YZYF_SPMC_PH("YZYF_SPMC_PH", "YzyfMatchBySpmcAndPhStrategy"),
    SPMC_MX("SPMC_MX", "MatchBySpmcAndMxStrategy"),
    SPMC("SPMC", "MatchBySpmcStrategy"),
    CGD_SPMC("CGD_SPMC", "MatchFpBySpmcStrategy"),
    CGD_SPMC_PH("CGD_SPMC", "MatchFpBySpmcAndPhStrategy"),
    SPMC_PZWH("SPMC_PZWH", "MatchBySpmcAndPzwhStrategy"),
    PH("PH", "MatchByPhStrategy"),
    FORCE_MATCH_SPMC_PH("FORCE_MATCH_SPMC_PH", "ForceMatchBySpmcAndPhStrategy"),
    FORCE_MATCH_SPMC_PH_RQ("FORCE_MATCH_SPMC_PH_RQ", "ForceMatchBySpmcAndPhAndRqStrategy"),
    FORCE_MATCH_SPMC_JE("FORCE_MATCH_SPMC_JE", "ForceMatchBySpmcAndJe"),
    FORCE_MATCH_JE("FORCE_MATCH_JE", "ForceMatchByJe"),




    //尾差
    SPMC_PH_JE_SL_WC("SPMC_PH_JE_SL_WC", "MatchBySpmcAndPh"),
    SPMC_PH_JE_SL_YZYF_FORCE("SPMC_PH_JE_SL_YZYF_FORCE", "MatchBySpmcAndPh"),
    SPMC_PH_JE_SL_WC_FORCE("SPMC_PH_JE_SL_WC_FORCE", "MatchBySpmcAndPh"),

    FPMX_CGDMX_SPMC_JE_SL("FPMX_CGDMX_SPMC_JE_SL", "MathchByFpMxAndCgdMx"),
    PH_SPMC_RQ_GROUP("PH_SPMC_RQ_GROUP", "MatchByPhAndSpmcAndRqGroup");





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
