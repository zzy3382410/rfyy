package com.current.rfyy.mapper;

import com.current.rfyy.domain.Cgd;
import com.current.rfyy.domain.CgdMx;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/1/9 15:21
 * @Description: TODO
 **/
@Mapper
public interface CgdMapper {

    /**
     * 根据销售方名称列表查询采购单列表
     *
     * @param xfmcList 销售方名称列表
     * @return 采购单列表
     */
    List<Cgd> selectCgdListByXfmcs(@Param("list") List<String> xfmcList, @Param("qq") String qq, @Param("qz") String qz);

    /**
     * 根据销售方名称列表查询采购单明细列表
     *
     * @param xfmcList 销售方名称列表
     * @return 采购单明细列表
     */
    List<CgdMx> selectCgdMxListByXfmcs(@Param("list") List<String> xfmcList, @Param("qq") String qq, @Param("qz") String qz);
}
