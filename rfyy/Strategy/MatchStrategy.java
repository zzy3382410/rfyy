package com.current.rfyy.Strategy;

import com.current.rfyy.domain.XsfMatchData;

/**
 * @Author: zzy
 * @Date: 2026/1/8 15:12
 * @Description: TODO 匹配策略
 **/
public interface MatchStrategy {

    /**
     * 执行匹配。
     *
     * @param xsf 企业级匹配上下文（数据 + 状态）
     * @param ctx 批次/全局上下文（命中收集、阈值、控制信息）
     * @return true  = 满足短路条件，终止后续策略
     * false = 继续执行后续策略
     */
    boolean match(XsfMatchData xsf, MatchContext ctx);

}
