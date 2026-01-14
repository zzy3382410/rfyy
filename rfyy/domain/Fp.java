package com.current.rfyy.domain;

import com.current.common.annotation.Excel;
import com.current.common.core.domain.BaseEntity;
import com.current.rfyy.constant.MatchStatus;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 全量发票池对象 jx_qlfp
 *
 * @author zzy
 * @date 2024-12-26
 */

@Data
public class Fp extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 序号
     */
    private Long id;

    /**
     * 发票代码
     */
    @Excel(name = "发票代码")
    private String fpdm;

    /**
     * 发票号码
     */
    @Excel(name = "发票号码")
    private String fphm;

    /**
     * 数电票号码
     */
    @Excel(name = "数电发票号码")
    private String sdphm;

    /**
     * 销方识别号
     */
    @Excel(name = "销方识别号")
    private String xfsbh;

    /**
     * 销方名称
     */
    @Excel(name = "销方名称")
    private String xfmc;

    /**
     * 购方识别号
     */
    @Excel(name = "购方识别号")
    private String gfsbh;

    /**
     * 购买方名称
     */
    @Excel(name = "购买方名称")
    private String gmfmc;

    /**
     * 开票日期
     */
    @Excel(name = "开票日期")
    private String kprq;

    /**
     * 特定业务类型
     */
    @Excel(name = "特定业务类型")
    private String tdywlx;

    /**
     * 金额
     */
    @Excel(name = "金额")
    private String je;

    /**
     * 税额
     */
    @Excel(name = "税额")
    private String se;

    /**
     * 价税合计
     */
    @Excel(name = "价税合计")
    private String jshj;

    /**
     * 发票来源
     */
    @Excel(name = "发票来源")
    private String fply;

    private String fplyDm;

    /**
     * 发票票种
     */
    @Excel(name = "发票票种")
    private String fppz;

    private String fppzDm;

    /**
     * 发票状态
     */
    @Excel(name = "发票状态")
    private String fpzt;

    private String fpztDm;

    /**
     * 是否正数发票
     */
    @Excel(name = "是否正数发票")
    private String sfzsfp;

    /**
     * 发票风险等级
     */
    @Excel(name = "发票风险等级")
    private String fpfxdj;

    private String fpfxdjDm;

    /**
     * 开票人
     */
    @Excel(name = "开票人")
    private String kpr;

    /**
     * 备注
     */
    @Excel(name = "备注")
    private String bz;


    /**
     * 报销状态
     */
    @Excel(name = "报销状态")
    private String bxzt;

    /**
     * 货物或应税劳务名称
     */
    @Excel(name = "货物或应税劳务名称")
    private String hwhyslwmc;

    /**
     * 发票所属企业纳税人识别号
     */
    private String nsrsbh;

    private String gjbq;

    /**
     * 发票明细
     */
    private List<FpMx> fpmxList;

    private String bcwjm;

    /**
     * 勾选状态
     */
    private String gxzt;

    /**
     * 勾选属期
     */
    private String gxsq;

    // 原始商品名称
    private String oriSpmc;

    // 处理后商品名称
    private String checkSpmc;

    // 发票上的商品数量
    private int spsl;

    private String matchedStrategy; // 可选：记录命中来源

    // 匹配结果 0：未匹配  1.自动匹配  2：手动匹配
    private String status;

    /**
     * 匹配打标
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
