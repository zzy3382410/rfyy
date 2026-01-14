package com.current.rfyy.domain;


import com.current.common.annotation.Excel;
import com.current.common.core.domain.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;

/**
 * cgd对象 t_cgd
 *
 * @author SiHan
 * @date 2025-02-12
 */
@Data
public class CgdMx extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 机构名称 */
    @Excel(name = "机构名称")
    private String jgmc;

    /** 日期 */
    @Excel(name = "日期")
    private String rq;

    /** 单据编号 */
    @Excel(name = "单据编号")
    private String djbh;

    /** 部门名称 */
    @Excel(name = "部门名称")
    private String bmmc;

    /** 单位编号 */
    @Excel(name = "单位编号")
    private String dwbh;

    /** 原单位编号 */
    @Excel(name = "原单位编号")
    private String ydwbh;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String gysmc;

    /** 税务分类编码 */
    @Excel(name = "税务分类编码")
    private String ssflbm;

    /** 商品编号 */
    @Excel(name = "商品编号")
    private String spbm;

    /** 原编号 */
    @Excel(name = "原编号")
    private String ybh;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String spmc;

    /** 商品规格 */
    @Excel(name = "商品规格")
    private String spgg;

    /** 生产厂家 */
    @Excel(name = "生产厂家")
    private String sccj;

    /** 批准文号 */
    @Excel(name = "批准文号")
    private String pzwh;

    /** 单位 */
    @Excel(name = "单位")
    private String dw;

    /** 采购数量 */
    @Excel(name = "采购数量")
    private Integer cgsl;

    /** 退补数量 */
    @Excel(name = "退补数量")
    private Integer tbsl;

    /** 含税价 */
    @Excel(name = "含税价")
    private BigDecimal hsj;

    /** 含税金额 */
    @Excel(name = "含税金额")
    private BigDecimal hsje;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal je;

    /** 税额 */
    @Excel(name = "税额")
    private BigDecimal se;

    /** 税率 */
    @Excel(name = "税率")
    private String sl;

    /** 商品资料进项税率 */
    @Excel(name = "商品资料进项税率")
    private String spzljxsl;

    /** 商品资料销项税率 */
    @Excel(name = "商品资料销项税率")
    private String spzlxxsl;

    /** 操作员 */
    @Excel(name = "操作员")
    private String czy;

    /** 序号 */
    @Excel(name = "序号")
    private String xh;

    /** 职员名称 */
    @Excel(name = "职员名称")
    private String zymc;

    /** 摘要 */
    @Excel(name = "摘要")
    private String zy;

    /** 库房名称 */
    @Excel(name = "库房名称")
    private String kfmc;

    /** 批号 */
    @Excel(name = "批号")
    private String ph;

    /** 生产日期 */
    @Excel(name = "生产日期")
    private String scrq;

    /** 有效期至 */
    @Excel(name = "有效期至")
    private String yyqz;

    /** 商品id */
    @Excel(name = "商品id")
    private String spid;

    /** 监管码 */
    @Excel(name = "监管码")
    private String jgm;

    /** 企业码 */
    @Excel(name = "企业码")
    private String qym;

    /** 商品流水号 */
    @Excel(name = "商品流水号")
    private String splsh;

    /** 最后含税进价 */
    @Excel(name = "最后含税进价")
    private String zhhsjj;


    /** 厂牌 */
    @Excel(name = "厂牌")
    private String cp;

    private String month;

    //命中标识
    private String targetFlag;

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


}
