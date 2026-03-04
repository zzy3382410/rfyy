package com.current.rfyy.utils;

import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.CgdMx;
import com.current.rfyy.domain.Fp;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * @Author: zzy
 * @Date: 2026/2/26 15:04
 * @Description: TODO 计算工具类
 **/
@Slf4j
public class CalcUtils {

    /**
     * 计算发票金额总和
     *
     * @param fpList 发票列表
     * @return 发票金额总和
     */
    public static BigDecimal calcFpJe(List<Fp> fpList) {
        if (fpList == null) {
            return BigDecimal.ZERO;
        }
        return fpList.stream()
                .map(fp -> fp != null ? fp.getJshjBd() : null)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    /**
     * 计算采购单明细金额总和
     *
     * @param cgdList 采购单明细列表
     * @return 采购单金额总和
     */
    public static BigDecimal calcCgdJe(List<Cgd> cgdList) {
        if (cgdList == null) {
            return BigDecimal.ZERO;
        }

        return cgdList.stream()
                .map(cgd -> cgd != null ? cgd.getHjjeBd() : null)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算采购单明细数量总和
     *
     * @param cgdList 采购单明细列表
     * @return 采购单数量总和
     */
    public static int calcCgdSl(List<Cgd> cgdList) {
        return cgdList.stream().mapToInt(Cgd::getCgsl).sum();
    }


    /**
     * 计算采购单明细数量总和
     *
     * @param cgdMxList 采购单明细列表
     * @return 采购单数量总和
     */
    public static int calcCgdMxSl(List<CgdMx> cgdMxList) {
        return cgdMxList.stream().mapToInt(CgdMx::getCgsl).sum();
    }

    /**
     * 计算采购单明细金额总和
     *
     * @param cgdMxList 采购单明细列表
     * @return 采购单金额总和
     */
    public static BigDecimal calcCgdMxJe(List<CgdMx> cgdMxList) {
        return cgdMxList.stream().map(CgdMx::getHsje).reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    /**
     * 比较发票金额和采购单金额是否相等，允许误差
     *
     * @param fpje  发票金额
     * @param cgdje 采购单金额
     * @param wc    是否允许尾差
     * @return 是否相等
     */
    public static boolean compareJe(BigDecimal fpje, BigDecimal cgdje, String wc) {
        if (RfyyConstant.WC_MATCH.equals(wc)) {
            return compareJeWithWc(fpje, cgdje) != null;
        } else {
            return compareJe(fpje, cgdje);
        }
    }


    /**
     * 比较发票金额和采购单金额是否相等，允许误差
     *
     * @param fpje  发票金额
     * @param cgdje 采购单金额
     * @return 是否相等
     */
    public static boolean compareJe(BigDecimal fpje, BigDecimal cgdje) {
        if (fpje == null || cgdje == null) {
            return false;
        }
        return (cgdje.subtract(fpje).abs().compareTo(RfyyConstant.AMOUNT_THRESHOLD) <= 0);
    }

    /**
     * 比较发票和采购单数量是否相等
     *
     * @param fpspsl  发票数量
     * @param cgdspsl 采购单数量
     * @return 是否相等
     */
    public static boolean compareSl(int fpspsl, int cgdspsl) {
        return fpspsl == cgdspsl;
    }

    /**
     * 比较发票金额和采购单金额、数量是否相等
     *
     * @param fpje  发票金额
     * @param cgdje 采购单金额
     * @return 是否相等
     */
    public static boolean compareJeAndSl(BigDecimal fpje, BigDecimal cgdje, int fpspsl, int cgdspsl) {
        return cgdje.subtract(fpje).abs().compareTo(RfyyConstant.AMOUNT_THRESHOLD) <= 0
                && fpspsl == cgdspsl;
    }

    /**
     * 比较发票金额和采购单金额是否相等，有尾差
     * 发票金额必须大于采购单金额
     * 且尾差小于设定值
     *
     * @param fpje  发票金额
     * @param cgdje 采购单金额
     * @return 是否相等
     */
    public static BigDecimal compareJeWithWc(BigDecimal fpje, BigDecimal cgdje) {
        // 采购单和发票金额的差值
        BigDecimal diff = fpje.subtract(cgdje);
        if (diff.abs().compareTo(RfyyConstant.AMOUNT_DIFF) <= 0) {
            return diff;
        } else {
            return null;
        }
    }


    public static BigDecimal compareJeWithWc(BigDecimal fpje, BigDecimal cgdje, BigDecimal wcje) {
        if (wcje == null) {
            wcje = RfyyConstant.AMOUNT_DIFF;
        }
        // 采购单和发票金额的差值
        BigDecimal diff = fpje.subtract(cgdje);
        if (diff.abs().compareTo(wcje) <= 0) {
            return diff;
        } else {
            return null;
        }
    }

}
