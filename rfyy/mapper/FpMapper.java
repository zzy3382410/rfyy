package com.current.rfyy.mapper;

import com.current.rfyy.domain.Fp;
import com.current.rfyy.domain.FpMx;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/1/9 15:21
 * @Description: TODO
 **/
@Mapper
public interface FpMapper {

    /**
     * 根据销售方名称列表查询发票列表
     *
     * @param xfmcList 销售方名称列表
     * @return 发票列表
     */
    List<Fp> selectFpListByXfmcs(@Param("list") List<String> xfmcList, @Param("qq") String qq, @Param("qz") String qz);

    /**
     * 根据销售方名称列表查询发票明细列表
     *
     * @param fpList 发票概要列表
     * @return 发票明细列表
     */
    List<FpMx> selectFpMxListByFpList(@Param("list") List<Fp> fpList);
}
