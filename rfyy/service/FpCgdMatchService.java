package com.current.rfyy.service;

import com.current.rfyy.domain.RfQueryDto;

/**
 * @Author: zzy
 * @Date: 2026/1/8 15:33
 * @Description: TODO
 **/
public interface FpCgdMatchService {

    /**
     * 匹配发票和采购订单分析结果
     */
    void matchFpCgd(RfQueryDto queryDto);
}
