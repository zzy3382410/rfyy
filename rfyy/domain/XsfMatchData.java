package com.current.rfyy.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @Author: zzy
 * @Date: 2026/1/8 15:17
 * @Description: TODO 供应商待匹配数据
 **/
@Data
public class XsfMatchData {

    /**
     * 供应商名称
     */
    private String xfmc;

    /**
     * 销方识别号
     */
    private String xfsbh;

    /**
     * 发票列表
     */
    private List<Fp> fpList;

    /**
     * 采购单列表
     */
    private List<Cgd> cgdList;

    // 存放该公司下的批号，根据采购单反向归集
    private Set<String> phs;

    // 未匹配的数据
    private List<Fp> remainingFp = new ArrayList<>();
    private List<Cgd> remainingCgd = new ArrayList<>();

    public void initRemaining() {
        remainingFp.clear();
        remainingCgd.clear();

        for (Fp fp : fpList) {
            if (!fp.isMatched()) {
                remainingFp.add(fp);
            }
        }
        for (Cgd cgd : cgdList) {
            if (!cgd.isMatched()) {
                remainingCgd.add(cgd);
            }
        }
    }

    public boolean allMatched() {
        return remainingFp.isEmpty() || remainingCgd.isEmpty();
    }

    //
    // //采购单命中率
    // private double  hits;
    //
    // //原始采购订单和发票的差额
    // private String difference;

}
