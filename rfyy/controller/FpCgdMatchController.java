package com.current.rfyy.controller;

import com.current.common.annotation.Anonymous;
import com.current.common.core.domain.AjaxResult;
import com.current.rfyy.domain.RfQueryDto;
import com.current.rfyy.service.FpCgdMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: zzy
 * @Date: 2026/1/8 15:31
 * @Description: TODO 发票采购单匹配控制器
 **/
@RestController
@RequestMapping("/rfyy")
@Slf4j
@RequiredArgsConstructor
public class FpCgdMatchController {

    private final FpCgdMatchService fpCgdMatchService;
    /**
     * 匹配发票和采购订单分析结果
     */
    @Anonymous
    @GetMapping(value = "/match")
    public AjaxResult match(RfQueryDto queryDto) {
        log.info("开始匹配数据------->");
        fpCgdMatchService.matchFpCgd(queryDto);
        return AjaxResult.success();
    }
}
