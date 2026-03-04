package com.current.rfyy.constant;

/**
 * @Author: zzy
 * @Date: 2026/1/9 11:11
 * @Description: TODO 匹配策略枚举
 **/
public enum MatchAlgorithmEnum {

    EACH("_EACH", "eachMatchAlgorithm"),
    TOTAL("_TOTAL", "totalMatchAlgorithm"),
    RQ_GROUP("_RQ_GROUP", "rqGroupMatchAlgorithm"),
    YZYF("_YZYF", "yzyfMatchAlgorithm"),
    FORCE("_FORCE", "forceMatchAlgorithm"),
    TOTAL_FP("_TOTAL_FP", "totalMatchFpAlgorithm"),
    RQ_GROUP_FP("_RQ_GROUP_FP", "rqGroupMatchFpAlgorithm"),
    FORCE_FP("_FORCE_FP", "forceMatchFpAlgorithm");



    private final String algorithm;
    private final String beanName;

    MatchAlgorithmEnum(String algorithm, String beanName) {
        this.algorithm = algorithm;
        this.beanName = beanName;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getBeanName() {
        return beanName;
    }
}
