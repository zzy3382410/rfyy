package com.current.rfyy.utils;

import com.current.rfyy.domain.Cgd;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工业级金额强制匹配工具类
 * 支持：
 * 1. 精确匹配
 * 2. 尾差匹配
 * 3. 自动算法选择（DFS / Meet-in-the-Middle）
 * 4. 日期分组匹配（选跨度最小）
 */
public class NewForceMatchUtils {

    /**
     * 自动切换阈值
     */
    private static final int DFS_THRESHOLD = 30;

    /* ==============================
            对外方法
     ============================== */

    public static List<Cgd> findFirstMatchByAmount(BigDecimal target,
                                                   List<Cgd> cgds) {
        return findFirstMatch(target, cgds, BigDecimal.ZERO);
    }

    public static List<Cgd> findFirstMatchByAmountWithWc(BigDecimal target,
                                                         List<Cgd> cgds,
                                                         BigDecimal wc) {
        return findFirstMatch(target, cgds, wc);
    }

    public static List<Cgd> findFirstMatch(BigDecimal target,
                                           List<Cgd> cgds,
                                           BigDecimal wc) {

        if (cgds == null || cgds.isEmpty() || cgds.size() > 60) {
            return Collections.emptyList();
        }

        if (wc == null) {
            wc = BigDecimal.ZERO;
        }

        long targetCent = toCent(target);
        long wcCent = toCent(wc.abs());

        List<Node> nodes = cgds.stream()
                .map(c -> new Node(c, toCent(c.getHjjeBd())))
                .collect(Collectors.toList());

        // 大金额优先
        nodes.sort((a, b) ->
                Long.compare(Math.abs(b.amount), Math.abs(a.amount)));

        if (nodes.size() <= DFS_THRESHOLD) {
            return dfsMatch(nodes, targetCent, wcCent);
        } else {
            return mitmMatch(nodes, targetCent, wcCent);
        }
    }

    /* ==============================
            Meet-in-the-Middle
     ============================== */

    private static List<Cgd> mitmMatch(List<Node> nodes,
                                       long target,
                                       long wc) {

        int n = nodes.size();
        int mid = n / 2;

        List<Node> left = nodes.subList(0, mid);
        List<Node> right = nodes.subList(mid, n);

        // 左侧子集
        List<Pair> leftSums = buildSubset(left);

        // 排序（为二分准备）
        leftSums.sort(Comparator.comparingLong(p -> p.sum));

        // 右侧枚举 + 二分
        List<Pair> rightSums = buildSubset(right);

        for (Pair r : rightSums) {

            long needMin = target - r.sum - wc;
            long needMax = target - r.sum + wc;

            int idx = lowerBound(leftSums, needMin);

            while (idx < leftSums.size()) {
                Pair l = leftSums.get(idx);

                if (l.sum > needMax) break;

                List<Cgd> result = new ArrayList<>();
                result.addAll(l.list);
                result.addAll(r.list);
                return result;

                // 如果要找最优解可以继续找
            }
        }

        return Collections.emptyList();
    }

    private static List<Pair> buildSubset(List<Node> nodes) {

        List<Pair> result = new ArrayList<>();
        int size = nodes.size();
        int total = 1 << size;

        for (int mask = 0; mask < total; mask++) {

            long sum = 0;
            List<Cgd> subset = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                if ((mask & (1 << i)) != 0) {
                    sum += nodes.get(i).amount;
                    subset.add(nodes.get(i).cgd);
                }
            }

            result.add(new Pair(sum, subset));
        }

        return result;
    }

    private static int lowerBound(List<Pair> list, long target) {

        int l = 0, r = list.size();

        while (l < r) {
            int m = (l + r) >>> 1;
            if (list.get(m).sum < target) {
                l = m + 1;
            } else {
                r = m;
            }
        }
        return l;
    }

    /* ==============================
                DFS剪枝版本
     ============================== */

    private static List<Cgd> dfsMatch(List<Node> nodes,
                                      long target,
                                      long wc) {

        int n = nodes.size();

        long[] remainMax = new long[n];
        long[] remainMin = new long[n];

        long max = 0, min = 0;

        for (int i = n - 1; i >= 0; i--) {
            long val = nodes.get(i).amount;
            if (val > 0) max += val;
            else min += val;
            remainMax[i] = max;
            remainMin[i] = min;
        }

        List<Cgd> path = new ArrayList<>();
        List<Cgd> result = new ArrayList<>();

        boolean found = dfs(nodes, 0, 0,
                target, wc,
                remainMax, remainMin,
                path, result);

        return found ? result : Collections.emptyList();
    }

    private static boolean dfs(List<Node> nodes,
                               int index,
                               long current,
                               long target,
                               long wc,
                               long[] remainMax,
                               long[] remainMin,
                               List<Cgd> path,
                               List<Cgd> result) {

        if (Math.abs(current - target) <= wc) {
            result.addAll(path);
            return true;
        }

        if (index >= nodes.size()) return false;

        if (current + remainMax[index] < target - wc) return false;
        if (current + remainMin[index] > target + wc) return false;

        for (int i = index; i < nodes.size(); i++) {

            path.add(nodes.get(i).cgd);

            if (dfs(nodes, i + 1,
                    current + nodes.get(i).amount,
                    target, wc,
                    remainMax, remainMin,
                    path, result)) {
                return true;
            }

            path.remove(path.size() - 1);
        }

        return false;
    }

    /* ==============================
           日期分组匹配
     ============================== */

    public static List<Cgd> findMatchByRqGroup(BigDecimal target,
                                               List<Cgd> cgds,
                                               BigDecimal wc) {

        if (cgds == null || cgds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Cgd>> group =
                cgds.stream().collect(Collectors.groupingBy(Cgd::getRq));

        List<GroupNode> nodes = group.entrySet().stream()
                .map(e -> new GroupNode(
                        e.getKey(),
                        e.getValue(),
                        e.getValue().stream()
                                .map(Cgd::getHjjeBd)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)))
                .collect(Collectors.toList());

        List<List<GroupNode>> solutions = new ArrayList<>();

        dfsGroup(nodes, 0,
                BigDecimal.ZERO,
                target,
                wc == null ? BigDecimal.ZERO : wc.abs(),
                new ArrayList<>(),
                solutions);

        if (solutions.isEmpty()) return Collections.emptyList();

        List<GroupNode> best =
                solutions.stream()
                        .min(Comparator.comparingLong(NewForceMatchUtils::dateSpan))
                        .orElse(Collections.emptyList());

        List<Cgd> result = new ArrayList<>();
        best.forEach(g -> result.addAll(g.cgds));

        return result;
    }

    private static void dfsGroup(List<GroupNode> groups,
                                 int index,
                                 BigDecimal current,
                                 BigDecimal target,
                                 BigDecimal wc,
                                 List<GroupNode> path,
                                 List<List<GroupNode>> solutions) {

        if (current.subtract(target).abs().compareTo(wc) <= 0) {
            solutions.add(new ArrayList<>(path));
            return;
        }

        if (index >= groups.size()) return;

        for (int i = index; i < groups.size(); i++) {

            path.add(groups.get(i));

            dfsGroup(groups, i + 1,
                    current.add(groups.get(i).total),
                    target, wc,
                    path, solutions);

            path.remove(path.size() - 1);
        }
    }

    private static long dateSpan(List<GroupNode> nodes) {

        if (nodes.size() <= 1) return 0;

        List<LocalDate> dates = nodes.stream()
                .map(n -> LocalDate.parse(n.rq))
                .sorted()
                .collect(Collectors.toList());

        return ChronoUnit.DAYS.between(
                dates.get(0),
                dates.get(dates.size() - 1));
    }

    /* ==============================
              内部结构
     ============================== */

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

    private static class Pair {
        long sum;
        List<Cgd> list;

        Pair(long sum, List<Cgd> list) {
            this.sum = sum;
            this.list = list;
        }
    }

    private static class GroupNode {
        String rq;
        List<Cgd> cgds;
        BigDecimal total;

        GroupNode(String rq,
                  List<Cgd> cgds,
                  BigDecimal total) {
            this.rq = rq;
            this.cgds = cgds;
            this.total = total;
        }
    }
}

