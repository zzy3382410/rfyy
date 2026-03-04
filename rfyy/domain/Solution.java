package com.current.rfyy.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/2/10 11:11
 * @Description: TODO 暴力匹配预处理结果
 **/
@Data
public class Solution {
    List<Cgd> cgdList;
    BigDecimal[] suffixAmount;
    boolean fscgd;


    public Solution(List<Cgd> cgdList,
                    BigDecimal[] suffixAmount,
                    boolean fscgd) {
        this.cgdList = cgdList;
        this.suffixAmount = suffixAmount;
        this.fscgd = fscgd;
    }
}
