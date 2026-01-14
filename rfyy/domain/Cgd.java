package com.current.rfyy.domain;


import com.current.common.annotation.Excel;
import com.current.common.core.domain.BaseEntity;
import com.current.rfyy.constant.MatchStatus;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * cgd对象 t_cgd
 *
 * @author SiHan
 * @date 2025-02-12
 */
@Data
public class Cgd extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "机构名称")
    private String jgmc;
    @Excel(name = "单据编号")
    private String djbh;
    @Excel(name = "供应商名称")
    private String gysmc;
    @Excel(name = "采购数量")
    private int cgsl;
    @Excel(name = "含税金额")
    private String cgdHjje;
    @Excel(name = "单据明细数量")
    private int cgdMxsl;

    //日期
    private String rq;

    // 原始对应的单据明细
    private List<CgdMx> cgdMxList;

    // 可选：记录命中来源
    private String matchedStrategy;

    // 匹配结果 0：未匹配  1.自动匹配  2：手动匹配
    private String status;

    /**
     *匹配打标
     */
    public void markMatched(String strategy) {
        this.status = MatchStatus.AUTO_MATCHED;
        this.matchedStrategy = strategy;
    }


    /**
     * 是否匹配中
     */
    public boolean isMatched() {
        return !StringUtils.isEmpty(this.status) && !MatchStatus.NOT_MATCHED.equals(this.status);
    }


}
