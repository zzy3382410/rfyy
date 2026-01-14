package com.current.rfyy.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: zzy
 * @Date: 2026/1/9 10:11
 * @Description: TODO 匹配结果
 **/
@Data
@NoArgsConstructor
public class MatchResult {

    private Long id;

    /**
     * 发票代码
     */
    private String fpdm;

    /**
     * 发票号码
     */
    private String fphm;

    /**
     * 数电发票号码
     */
    private String sdphm;

    /**
     * 采购单编号
     */
    private String djbh;

    /**
     * 命中策略
     */
    private String strategy;

    /**
     * 命中状态
     */
    private String status;

    /**
     * 命中时间
     */
    private LocalDateTime matchTime;

    /**
     * 操作人
     */
    private String matchBy;


    public MatchResult(String fpdm, String fphm, String sdphm, String djbh, String strategy, String status, LocalDateTime matchTime, String matchBy) {
        this.fpdm = fpdm;
        this.fphm = fphm;
        this.sdphm = sdphm;
        this.djbh = djbh;
        this.strategy = strategy;
        this.status = status;
        this.matchTime = matchTime;
        this.matchBy = matchBy;
    }
}
