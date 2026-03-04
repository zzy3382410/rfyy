package com.current.rfyy.domain;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

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

    /**
     * 采购单候选索引（一次构建，全策略复用）
     */
    private final Map<String, Set<Cgd>> cgdIndexByPh = new HashMap<>();
    private final Map<String, Set<Cgd>> cgdIndexBySpmc = new HashMap<>();
    private final Map<String, Set<Cgd>> cgdIndexByHandledSpmc = new HashMap<>();
    private final Map<String, Set<Cgd>> cgdIndexByRq = new HashMap<>();
    private boolean cgdIndexBuilt;

    public void initRemaining() {
        ensureCgdIndexes();

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

    public Set<Cgd> getCgdByPh(String ph) {
        return cgdIndexByPh.getOrDefault(normalize(ph), Collections.emptySet());
    }

    public Set<Cgd> getCgdBySpmc(String spmc) {
        return cgdIndexBySpmc.getOrDefault(normalize(spmc), Collections.emptySet());
    }

    public Set<Cgd> getCgdByHandledSpmc(String handledSpmc) {
        return cgdIndexByHandledSpmc.getOrDefault(normalize(handledSpmc), Collections.emptySet());
    }

    public Set<Cgd> getCgdByRq(String rq) {
        return cgdIndexByRq.getOrDefault(normalize(rq), Collections.emptySet());
    }

    /**
     * 当采购单从 remaining 池移除时，同步清理索引，避免命中已匹配数据
     */
    public void removeCgdFromIndexes(Cgd cgd) {
        if (!cgdIndexBuilt || cgd == null) {
            return;
        }
        removeFromIndex(cgdIndexByRq, cgd.getRq(), cgd);
        List<CgdMx> mxList = cgd.getCgdMxList();
        if (mxList == null) {
            return;
        }
        for (CgdMx mx : mxList) {
            removeFromIndex(cgdIndexByPh, mx.getPh(), cgd);
            removeFromIndex(cgdIndexBySpmc, mx.getSpmc(), cgd);
            removeFromIndex(cgdIndexByHandledSpmc, mx.getHandledSpmc(), cgd);
        }
    }

    private void ensureCgdIndexes() {
        if (cgdIndexBuilt || cgdList == null) {
            return;
        }
        for (Cgd cgd : cgdList) {
            if (cgd == null) {
                continue;
            }
            addIndex(cgdIndexByRq, cgd.getRq(), cgd);
            List<CgdMx> mxList = cgd.getCgdMxList();
            if (mxList == null) {
                continue;
            }
            for (CgdMx mx : mxList) {
                addIndex(cgdIndexByPh, mx.getPh(), cgd);
                addIndex(cgdIndexBySpmc, mx.getSpmc(), cgd);
                addIndex(cgdIndexByHandledSpmc, mx.getHandledSpmc(), cgd);
            }
        }
        cgdIndexBuilt = true;
    }

    private void addIndex(Map<String, Set<Cgd>> index, String key, Cgd cgd) {
        String normalized = normalize(key);
        if (normalized == null) {
            return;
        }
        index.computeIfAbsent(normalized, k -> new LinkedHashSet<>()).add(cgd);
    }

    private void removeFromIndex(Map<String, Set<Cgd>> index, String key, Cgd cgd) {
        String normalized = normalize(key);
        if (normalized == null) {
            return;
        }
        Set<Cgd> bucket = index.get(normalized);
        if (bucket == null) {
            return;
        }
        bucket.remove(cgd);
        if (bucket.isEmpty()) {
            index.remove(normalized);
        }
    }

    private String normalize(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    //
    // //采购单命中率
    // private double  hits;
    //
    // //原始采购订单和发票的差额
    // private String difference;

}
