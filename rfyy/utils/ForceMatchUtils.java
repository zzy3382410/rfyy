package com.current.rfyy.utils;

import com.current.rfyy.constant.RfyyConstant;
import com.current.rfyy.domain.Cgd;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/2/11 15:37
 * @Description: TODO
 **/
public class ForceMatchUtils {

    public static List<Cgd> findFirstMatchByAmount(BigDecimal fpJshj,
                                                   List<Cgd> cgds,
                                                   String wc) {
        if (RfyyConstant.WC_MATCH.equals(wc)) {
            return findFirstMatch(fpJshj, cgds, RfyyConstant.AMOUNT_DIFF);
        } else {
            return findFirstMatch(fpJshj, cgds, BigDecimal.ZERO);
        }
    }


    /**
     * 精确匹配（不允许尾差）
     */
    public static List<Cgd> findFirstMatchByAmount(BigDecimal fpJshj,
                                                   List<Cgd> cgds) {
        return findFirstMatch(fpJshj, cgds, BigDecimal.ZERO);
    }

    /**
     * 允许尾差匹配
     */
    public static List<Cgd> findFirstMatchByAmountWithWc(BigDecimal fpJshj,
                                                         List<Cgd> cgds,
                                                         BigDecimal wc) {
        if (wc == null) {
            wc = BigDecimal.ZERO;
        }
        return findFirstMatch(fpJshj, cgds, wc.abs());
    }

    /**
     * 统一引擎（核心）
     */
    /**
     * 工业级匹配引擎
     */
    public static List<Cgd> findFirstMatch(BigDecimal target,
                                           List<Cgd> cgds,
                                           BigDecimal wc) {

        if (cgds == null || cgds.isEmpty() || cgds.size() > RfyyConstant.MAX_CANDIDATE_SIZE) {
            return Collections.emptyList();
        }

        if (wc == null) {
            wc = BigDecimal.ZERO;
        }

        // 金额转分（避免 BigDecimal 递归慢）
        long targetCent = toCent(target);
        long wcCent = toCent(wc.abs());

        List<Node> nodes = new ArrayList<>();

        for (Cgd cgd : cgds) {
            nodes.add(new Node(cgd, toCent(cgd.getHjjeBd())));
        }

        // 大金额优先（绝对值降序）
        nodes.sort((a, b) -> Long.compare(
                Math.abs(b.amount),
                Math.abs(a.amount)
        ));

        int n = nodes.size();

        long[] remainingMax = new long[n];
        long[] remainingMin = new long[n];

        long max = 0;
        long min = 0;

        for (int i = n - 1; i >= 0; i--) {
            long val = nodes.get(i).amount;

            if (val > 0) {
                max += val;
            } else {
                min += val;
            }

            remainingMax[i] = max;
            remainingMin[i] = min;
        }

        List<Cgd> result = new ArrayList<>();
        List<Cgd> path = new ArrayList<>();

        boolean found = dfs(
                nodes,
                0,
                0L,
                targetCent,
                wcCent,
                remainingMax,
                remainingMin,
                path,
                result
        );

        return found ? result : Collections.emptyList();
    }

    /**
     * 按照日期分组后，组层面做匹配，选日期跨度最小的组组合
     */
    public static List<Cgd> findMatchByRqGroup(
            BigDecimal target,
            List<Cgd> cgds,
            BigDecimal wc) {

        if (cgds == null || cgds.isEmpty()) {
            return Collections.emptyList();
        }

        if (wc == null) {
            wc = BigDecimal.ZERO;
        }

        // 1️⃣ 按日期分组
        Map<String, List<Cgd>> rqGroup =
                cgds.stream()
                        .collect(Collectors.groupingBy(Cgd::getRq));

        // 2️⃣ 构建组节点
        List<GroupNode> groupNodes = rqGroup.entrySet().stream()
                .map(e -> new GroupNode(
                        e.getKey(),
                        e.getValue(),
                        e.getValue().stream()
                                .map(Cgd::getHjjeBd)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .collect(Collectors.toList());

        // 3️⃣ 在组层面做匹配
        SearchState state = new SearchState();

        dfsBest(
                groupNodes,
                0,
                BigDecimal.ZERO,
                target,
                wc.abs(),
                new ArrayList<>(),
                state,
                null,
                null
        );

        if (state.bestPath.isEmpty()) {
            return Collections.emptyList();
        }

        // 5️⃣ 展开为单据
        List<Cgd> result = new ArrayList<>();
        for (GroupNode node : state.bestPath) {
            result.addAll(node.cgds);
        }

        return result;
    }

    /**
     * DFS 搜索所有组组合
     */
    private static void dfsBest(List<GroupNode> groups,
                                int index,
                                BigDecimal current,
                                BigDecimal target,
                                BigDecimal wc,
                                List<GroupNode> path,
                                SearchState state,
                                LocalDate minDate,
                                LocalDate maxDate) {

        BigDecimal diff = current.subtract(target).abs();

        if (diff.compareTo(wc) <= 0) {
            long span = calcSpan(minDate, maxDate);
            if (span < state.bestSpan) {
                state.bestSpan = span;
                state.bestPath = new ArrayList<>(path);
            }
            return;
        }

        if (index >= groups.size()) {
            return;
        }

        for (int i = index; i < groups.size(); i++) {

            GroupNode node = groups.get(i);

            path.add(node);

            LocalDate nextMin = minDate;
            LocalDate nextMax = maxDate;
            if (node.date != null) {
                if (nextMin == null || node.date.isBefore(nextMin)) {
                    nextMin = node.date;
                }
                if (nextMax == null || node.date.isAfter(nextMax)) {
                    nextMax = node.date;
                }
            }

            long currentSpan = calcSpan(nextMin, nextMax);
            if (currentSpan > state.bestSpan) {
                path.remove(path.size() - 1);
                continue;
            }

            dfsBest(
                    groups,
                    i + 1,
                    current.add(node.total),
                    target,
                    wc,
                    path,
                    state,
                    nextMin,
                    nextMax
            );

            path.remove(path.size() - 1);
        }
    }

    private static long calcSpan(LocalDate minDate, LocalDate maxDate) {
        if (minDate == null || maxDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(minDate, maxDate);
    }

    /**
     * 日期组节点
     */
    private static class GroupNode {

        String rq;
        List<Cgd> cgds;
        BigDecimal total;
        LocalDate date;

        GroupNode(String rq,
                  List<Cgd> cgds,
                  BigDecimal total) {
            this.rq = rq;
            this.cgds = cgds;
            this.total = total;
            if (rq != null && !rq.isBlank()) {
                this.date = LocalDate.parse(rq);
            }
        }
    }

    private static class SearchState {
        long bestSpan = Long.MAX_VALUE;
        List<GroupNode> bestPath = Collections.emptyList();
    }


    private static boolean dfs(List<Node> nodes,
                               int index,
                               long current,
                               long target,
                               long wc,
                               long[] remainingMax,
                               long[] remainingMin,
                               List<Cgd> path,
                               List<Cgd> result) {

        long diff = Math.abs(current - target);
        if (diff <= wc) {
            result.addAll(path);
            return true;
        }

        if (index >= nodes.size()) {
            return false;
        }

        // 安全剪枝1：最大不够
        if (current + remainingMax[index] < target - wc) {
            return false;
        }

        // 安全剪枝2：最小超了
        if (current + remainingMin[index] > target + wc) {
            return false;
        }

        for (int i = index; i < nodes.size(); i++) {

            Node node = nodes.get(i);

            path.add(node.cgd);

            if (dfs(
                    nodes,
                    i + 1,
                    current + node.amount,
                    target,
                    wc,
                    remainingMax,
                    remainingMin,
                    path,
                    result
            )) {
                return true;
            }

            path.remove(path.size() - 1);
        }

        return false;
    }

    private static long toCent(BigDecimal val) {
        return val.multiply(BigDecimal.valueOf(100)).longValue();
    }

    private static class Node {
        Cgd cgd;
        long amount;

        Node(Cgd cgd, long amount) {
            this.cgd = cgd;
            this.amount = amount;
        }
    }
}
