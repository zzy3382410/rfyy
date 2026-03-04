package com.current.rfyy.utils;

import com.current.rfyy.domain.Fp;

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
public class ForceMatchFpUtils {

    /**
     * 自动切换阈值
     */
    private static final int DFS_THRESHOLD = 30;
    /**
     * Meet-in-the-Middle 的半边上限，防止 2^n 子集过大导致堆内存膨胀
     */
    private static final int MITM_HALF_LIMIT = 22;

    /* ==============================
            对外方法
     ============================== */

    public static List<Fp> findFirstMatchByAmount(BigDecimal target,
                                                  List<Fp> fps) {
        return findFirstMatch(target, fps, BigDecimal.ZERO);
    }

    public static List<Fp> findFirstMatchByAmountWithWc(BigDecimal target,
                                                        List<Fp> fps,
                                                        BigDecimal wc) {
        return findFirstMatch(target, fps, wc);
    }

    public static List<Fp> findFirstMatch(BigDecimal target,
                                          List<Fp> fps,
                                          BigDecimal wc) {

        if (fps == null || fps.isEmpty() || fps.size() > 60) {
            return Collections.emptyList();
        }

        if (wc == null) {
            wc = BigDecimal.ZERO;
        }

        long targetCent = toCent(target);
        long wcCent = toCent(wc.abs());

        List<Node> nodes = fps.stream()
                .map(c -> new Node(c, toCent(c.getJshjBd())))
                .collect(Collectors.toList());

        // 大金额优先
        nodes.sort((a, b) ->
                Long.compare(Math.abs(b.amount), Math.abs(a.amount)));

        if (nodes.size() <= DFS_THRESHOLD) {
            return dfsMatch(nodes, targetCent, wcCent);
        }

        int mitmHalfSize = nodes.size() / 2;
        if (mitmHalfSize > MITM_HALF_LIMIT) {
            // 大规模数据优先避免内存爆炸，退化到 DFS（时间更长但更稳）
            return dfsMatch(nodes, targetCent, wcCent);
        } else {
            return mitmMatch(nodes, targetCent, wcCent);
        }
    }

    /* ==============================
            Meet-in-the-Middle
     ============================== */

    private static List<Fp> mitmMatch(List<Node> nodes,
                                      long target,
                                      long wc) {

        int n = nodes.size();
        int mid = n / 2;

        List<Node> left = nodes.subList(0, mid);
        List<Node> right = nodes.subList(mid, n);

        // 左侧子集（仅保存 sum + mask，避免为每个子集创建 List）
        List<Pair> leftSums = buildSubset(left, 0);

        // 排序（为二分准备）
        leftSums.sort(Comparator.comparingLong(p -> p.sum));

        // 右侧按 mask 在线枚举 + 二分，避免额外构建右侧全部子集对象
        int rightSize = right.size();
        long rightTotal = 1L << rightSize;

        for (long rightMask = 0; rightMask < rightTotal; rightMask++) {

            long rightSum = subsetSum(right, rightMask);

            long needMin = target - rightSum - wc;
            long needMax = target - rightSum + wc;

            int idx = lowerBound(leftSums, needMin);

            while (idx < leftSums.size()) {
                Pair l = leftSums.get(idx);

                if (l.sum > needMax) break;

                return assembleResult(left, right, l.mask, rightMask);

                // 如果要找最优解可以继续找
            }
        }

        return Collections.emptyList();
    }

    private static List<Pair> buildSubset(List<Node> nodes, int offset) {

        List<Pair> result = new ArrayList<>();
        int size = nodes.size();
        long total = 1L << size;

        for (long mask = 0; mask < total; mask++) {

            result.add(new Pair(subsetSum(nodes, mask), mask << offset));
        }

        return result;
    }

    private static long subsetSum(List<Node> nodes, long mask) {
        long sum = 0;
        for (int i = 0; i < nodes.size(); i++) {
            if ((mask & (1L << i)) != 0) {
                sum += nodes.get(i).amount;
            }
        }
        return sum;
    }

    private static List<Fp> assembleResult(List<Node> left,
                                           List<Node> right,
                                           long leftMask,
                                           long rightMask) {
        List<Fp> result = new ArrayList<>();

        for (int i = 0; i < left.size(); i++) {
            if ((leftMask & (1L << i)) != 0) {
                result.add(left.get(i).fp);
            }
        }

        for (int i = 0; i < right.size(); i++) {
            if ((rightMask & (1L << i)) != 0) {
                result.add(right.get(i).fp);
            }
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

    private static List<Fp> dfsMatch(List<Node> nodes,
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

        List<Fp> path = new ArrayList<>();
        List<Fp> result = new ArrayList<>();

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
                               List<Fp> path,
                               List<Fp> result) {

        if (Math.abs(current - target) <= wc) {
            result.addAll(path);
            return true;
        }

        if (index >= nodes.size()) return false;

        if (current + remainMax[index] < target - wc) return false;
        if (current + remainMin[index] > target + wc) return false;

        for (int i = index; i < nodes.size(); i++) {

            path.add(nodes.get(i).fp);

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

    public static List<Fp> findMatchByRqGroup(BigDecimal target,
                                              List<Fp> fps,
                                              BigDecimal wc) {

        if (fps == null || fps.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Fp>> group =
                fps.stream().collect(Collectors.groupingBy(fp -> fp.getKprq().substring(0, 10)));

        List<GroupNode> nodes = group.entrySet().stream()
                .map(e -> new GroupNode(
                        e.getKey(),
                        e.getValue(),
                        e.getValue().stream()
                                .map(Fp::getJshjBd)
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
                        .min(Comparator.comparingLong(ForceMatchFpUtils::dateSpan))
                        .orElse(Collections.emptyList());

        List<Fp> result = new ArrayList<>();
        best.forEach(g -> result.addAll(g.fps));

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
        Fp fp;
        long amount;

        Node(Fp fp, long amount) {
            this.fp = fp;
            this.amount = amount;
        }
    }

    private static class Pair {
        long sum;
        long mask;

        Pair(long sum, long mask) {
            this.sum = sum;
            this.mask = mask;
        }
    }

    private static class GroupNode {
        String rq;
        List<Fp> fps;
        BigDecimal total;

        GroupNode(String rq,
                  List<Fp> fps,
                  BigDecimal total) {
            this.rq = rq;
            this.fps = fps;
            this.total = total;
        }
    }
}
