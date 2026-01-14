package com.current.rfyy.mapper;

import com.current.rfyy.domain.MatchResult;
import com.current.rfyy.domain.RfQueryDto;
import com.current.rfyy.domain.RfXsfTotal;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/1/8 15:59
 * @Description: TODO
 **/
@Mapper
public interface FpCgdMatchMapper {

    /**
     * 查询销售方列表
     *
     * @param queryDto 查询参数
     * @return 销售方列表
     */
    List<RfXsfTotal> selectXfmcTotalList(RfQueryDto queryDto);

    /**
     * 批量插入匹配结果
     *
     * @param list 匹配结果列表
     */
    void batchInsert(List<MatchResult> list);
}
