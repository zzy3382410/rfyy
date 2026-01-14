package com.current.rfyy.service.impl;

import com.current.rfyy.Strategy.*;
import com.current.rfyy.batch.BatchConfig;
import com.current.rfyy.batch.BatchProcessor;
import com.current.rfyy.domain.*;
import com.current.rfyy.mapper.CgdMapper;
import com.current.rfyy.mapper.FpCgdMatchMapper;
import com.current.rfyy.mapper.FpMapper;
import com.current.rfyy.service.FpCgdMatchService;
import com.current.rfyy.utils.MatchUtils;
import com.current.rfyy.utils.SplitUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/1/8 15:33
 * @Description: TODO 发票采购单匹配service
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class FpCgdMatchServiceImpl implements FpCgdMatchService {

    private final ThreadPoolTaskExecutor fpCgdMatchExecutor;
    private final FpCgdMatchMapper fpCgdMatchMapper;
    private final FpMapper fpMapper;
    private final CgdMapper cgdMapper;

    @Override
    public void matchFpCgd(RfQueryDto queryDto) {
        // 获取所有销售方列表
        List<String> xsfList = getAllXsfList(queryDto);
        // 分批处理
        processAllXsf(xsfList);
    }

    public void processAllXsf(List<String> xfmcList) {

        // 可以动态配置，比如通过 application.yml
        BatchConfig config = BatchConfig.builder()
                .batchSize(100)              // 每批处理100企业
                .threadCount(10)              // 10线程并行
                // .maxInSize(200)              // 单次 SQL IN 不超过200
                .maxRetry(0)                 // 错误重试 0 次
                .build();

        BatchProcessor<String> processor = new BatchProcessor<>(config, fpCgdMatchExecutor);

        processor.processInBatches(
                xfmcList,
                this::processBatch,   // 批处理逻辑（你要写的）
                this::handleBatchError       // 批错误处理
        );
    }

    /**
     * 处理批次的企业数据
     *
     * @param xfmcBatch 企业批次
     */
    public void processBatch(List<String> xfmcBatch) {

        log.info("开始处理企业批次: {}", xfmcBatch);

        // 1. 批量取出所有数据（分片查询）
        List<Fp> fpList = executeInChunks(
                xfmcBatch,
                1000,
                true,
                fpMapper::selectFpListByXfmcs
        );
        List<Cgd> cgdList = executeInChunks(
                xfmcBatch,
                1000,
                true,
                cgdMapper::selectCgdListByXfmcs
        );
        List<FpMx> fpMxList = executeInChunks(
                xfmcBatch,
                1000,
                true,
                fpMapper::selectFpMxListByXfmcs
        );
        List<CgdMx> cgdMxList = executeInChunks(
                xfmcBatch,
                1000,
                true,
                cgdMapper::selectCgdMxListByXfmcs
        );

        // 2. 构建企业数据的索引 map
        Map<String, List<Fp>> fpMap = fpList.stream().collect(Collectors.groupingBy(Fp::getXfmc));
        Map<String, List<Cgd>> cgdMap = cgdList.stream().collect(Collectors.groupingBy(Cgd::getGysmc));
        Map<String, List<FpMx>> oriFpMap = fpMxList.stream().collect(Collectors.groupingBy(FpMx::getXfmc));
        Map<String, List<CgdMx>> oriCgdMap = cgdMxList.stream().collect(Collectors.groupingBy(CgdMx::getGysmc));

        // 3. 为每个企业构建 XsfVO 并执行所有匹配策略
        List<MatchResult> allMatchResultList = new ArrayList<>();
        for (String xfmc : xfmcBatch) {
            XsfMatchData xsf = buildXsfData(
                    xfmc,
                    fpMap.getOrDefault(xfmc, List.of()),
                    cgdMap.getOrDefault(xfmc, List.of()),
                    oriFpMap.getOrDefault(xfmc, List.of()),
                    oriCgdMap.getOrDefault(xfmc, List.of())
            );

            // 执行匹配策略链
            MatchEngine engine = new MatchEngine();
            MatchContext ctx = engine.match(xsf);
            allMatchResultList.addAll(ctx.getMatchResults());
        }

        // 4. 写入数据库或输出结果
        saveBatchMatchResults(allMatchResultList);


        log.info("批次处理完成，共 {} 企业", xfmcBatch.size());
    }


    /**
     * 获取所有销售方列表
     *
     * @param queryDto 查询参数
     *
     */
    private List<String> getAllXsfList(RfQueryDto queryDto) {
        // 查询所有销售方列表
        List<RfXsfTotal> xsfTotalList = fpCgdMatchMapper.selectXfmcTotalList(queryDto);

        // 过滤没有发票或者没有采购单的数据
        return xsfTotalList.stream()
                .filter(xsf -> xsf.getFpCount() > 0 && xsf.getCgdCount() > 0)
                .map(RfXsfTotal::getXfmc)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }


    /**
     * 分批查询列表
     */
    public <T, R> List<R> executeInChunks(List<T> sourceList, int chunkSize, boolean parallel, Function<List<T>, List<R>> queryFunction) {
        if (sourceList == null || sourceList.isEmpty()) return new ArrayList<>();

        List<List<T>> chunks = SplitUtils.split(sourceList, chunkSize);

        var stream = parallel ? chunks.parallelStream() : chunks.stream();

        return stream.map(queryFunction)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * 构建单个销售方的匹配数据
     *
     * @param xfmc      销售方名称
     * @param fpList    发票列表
     * @param cgdList   采购单列表
     * @param fpMxList  发票明细列表
     * @param cgdMxList 采购单明细列表
     * @return 销售方匹配数据
     */
    private XsfMatchData buildXsfData(String xfmc, List<Fp> fpList, List<Cgd> cgdList, List<FpMx> fpMxList, List<CgdMx> cgdMxList) {
        XsfMatchData xsf = new XsfMatchData();
        xsf.setXfmc(xfmc);
        // sdphm_fpdm_fphm 做发票明细的索引
        Map<String, List<FpMx>> fpmxMap = fpMxList.stream()
                .collect(Collectors.groupingBy(
                        fpmx -> String.format("%s_%s_%s", fpmx.getSdphm(), fpmx.getFpdm(), fpmx.getFphm())
                ));

        for (Fp fp : fpList) {
            String key = String.format("%s_%s_%s", fp.getSdphm(), fp.getFpdm(), fp.getFphm());
            List<FpMx> fpMxs = fpmxMap.getOrDefault(key, Collections.emptyList());
            fp.setFpmxList(fpMxs);

            if (fpMxs.isEmpty()) {
                continue;
            }
            // 商品名称处理
            Set<String> spmcSet = new LinkedHashSet<>();
            Set<String> checkSpmcSet = new LinkedHashSet<>();
            fpMxs.forEach(fpMx -> {
                String hwlwmc = fpMx.getHwhyslwmc();
                if (StringUtils.isNotEmpty(hwlwmc)) {
                    spmcSet.add(hwlwmc);
                }
                // 清洗后商品名
                String handled = MatchUtils.handleSpmc(hwlwmc);
                if (handled != null && !handled.isEmpty()) {
                    checkSpmcSet.add(handled);
                }

            });
            fp.setOriSpmc(String.join("_", spmcSet));
            fp.setCheckSpmc(String.join("_", checkSpmcSet));
        }

        // djbh 采购单明细索引
        Map<String, List<CgdMx>> cgdMxMap = cgdMxList.stream()
                .collect(Collectors.groupingBy(
                        CgdMx::getDjbh
                ));
        for (Cgd cgd : cgdList) {
            List<CgdMx> cgdMxs = cgdMxMap.getOrDefault(cgd.getDjbh(), Collections.emptyList());
            cgd.setCgdMxList(cgdMxs);
            if (!CollectionUtils.isEmpty(cgdMxs)) {
                cgd.setRq(cgdMxs.get(0).getRq());
            }
        }
        // 获取批号集合
        Set<String> phset = new LinkedHashSet<>();
        cgdMxList.forEach(cgdMx -> phset.add(cgdMx.getPh()));
        xsf.setFpList(fpList);
        xsf.setCgdList(cgdList);
        xsf.setPhs(phset);
        return xsf;
    }


    /**
     * 批量保存匹配结果
     *
     * @param matchResults 匹配结果列表
     */
    public void saveBatchMatchResults(List<MatchResult> matchResults) {
        if (matchResults.isEmpty()) return;

        // 分批 500
        List<List<MatchResult>> parts = SplitUtils.split(matchResults, 500);

        for (List<MatchResult> part : parts) {
            fpCgdMatchMapper.batchInsert(part);
        }
    }


    /**
     * 异常处理
     *
     * @param e 异常
     */
    public void handleBatchError(Throwable e) {
        log.error("批处理失败，已自动熔断此批", e);
        // 可写入消息队列、报警、保存失败企业清单等
    }

    //
    // /**
    //  * 执行所有匹配策略
    //  *
    //  * @param xsf 销售方数据
    //  * @return 匹配结果列表
    //  */
    // public List<MatchResult> matchAllStrategies(XsfMatchData xsf) {
    //     // 初始化所有匹配策略
    //     List<MatchStrategy> strategies = List.of(
    //             new MatchBySpmcAndPhAndJeAndSl(),
    //             new MatchBySpmcAndJeAndSlAndRq(),
    //             new MatchBySpmcAndJeAndSl()
    //     );
    //     MatchContext ctx = new MatchContext();
    //     xsf.initRemaining();
    //
    //     for (MatchStrategy strategy : strategies) {
    //         boolean done = false;
    //         try {
    //             done = strategy.match(xsf, ctx);
    //         } catch (Exception e) {
    //             log.error("企业：{},匹配策略:{} 执行异常: {}", xsf.getXfmc(), strategy.getClass().getSimpleName(), e.getMessage(), e);
    //         }
    //         if (done || xsf.allMatched()) {
    //             break;
    //         }
    //     }
    //     return ctx.getMatchResults();
    // }

}
