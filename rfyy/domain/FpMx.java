package com.current.rfyy.domain;


import com.current.common.annotation.Excel;
import com.current.common.core.domain.BaseEntity;
import lombok.Data;


/**
 * @ClassName: JxQlfp
 * @Author: zzy
 * @Date: 2024/12/26 9:04
 * @Description:
 */
@Data
public class FpMx extends BaseEntity {

    private Long id;

    private Long zbId;

    @Excel(name = "发票代码")
    private String fpdm;

    @Excel(name = "发票号码")
    private String fphm;

    @Excel(name = "数电发票号码")
    private String sdphm;

    @Excel(name = "销方识别号")
    private String xfsbh;

    @Excel(name = "销方名称")
    private String xfmc;

    @Excel(name = "购方识别号")
    private String gfsbh;

    @Excel(name = "购买方名称")
    private String gmfmc;

    @Excel(name = "开票日期")
    private String kprq;

    @Excel(name = "税收分类编码")
    private String ssflbm;

    @Excel(name = "特定业务类型")
    private String tdywlx;

    @Excel(name = "货物或应税劳务名称")
    private String hwhyslwmc;

    @Excel(name = "规格型号")
    private String ggxh;

    @Excel(name = "单位")
    private String dw;

    @Excel(name = "数量")
    private String sl;

    @Excel(name = "单价")
    private String dj;

    @Excel(name = "金额")
    private String je;

    @Excel(name = "税率")
    private String slv;

    @Excel(name = "税额")
    private String se;

    @Excel(name = "价税合计")
    private String jshj;

    @Excel(name = "发票来源")
    private String fply;
    private String fplyDm;

    @Excel(name = "发票票种")
    private String fppz;
    private String fppzDm;

    @Excel(name = "发票状态")
    private String fpzt;
    private String fpztDm;

    @Excel(name = "是否正数发票")
    private String sfzsfp;

    @Excel(name = "发票风险等级")
    private String fpfxdj;
    private String fpfxdjDm;

    @Excel(name = "开票人")
    private String kpr;

    @Excel(name = "备注")
    private String bz;

    /**
     * 货物或应税劳务名称1
     */
    @Excel(name = "货物或应税劳务名称1")
    private String hwhyslwmc1;

    /**
     * 发票所属企业纳税人识别号
     */
    private String nsrsbh;

    private String gjbq;


}
