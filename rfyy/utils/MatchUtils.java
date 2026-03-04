package com.current.rfyy.utils;

import com.current.rfyy.Strategy.MatchContext;
import com.current.rfyy.constant.StrategyEnum;
import com.current.rfyy.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author: zzy
 * @Date: 2026/1/9 11:47
 * @Description: TODO
 **/
@Slf4j
public class MatchUtils {

    /**
     * 是否退补采购单
     * 采购退补价执行
     */
    public static boolean isTbCgd(Cgd cgd) {
        boolean ph = cgd.getCgdMxList().stream().anyMatch(mx -> "采购退补价执行".equals(mx.getZy()));
        return cgd.getHjjeBd().compareTo(BigDecimal.ZERO) <= 0 && ph;
    }


    /**
     * 校验商品名称和规格型号是否有效
     *
     * @param spmc 商品名称
     * @param ph   规格型号
     * @return 是否有效
     */
    public static boolean isValid(String spmc, String ph) {
        return spmc != null && !spmc.isBlank()
                && ph != null && !ph.isBlank();
    }

    /**
     * 根据采购单批号规则，从发票备注中匹配出所有可能的批号
     * 根据找到公司所有的批号，来反向看发票的备注中是否存在批号，如若有则取出。
     * 因很多的发票备注中不规范，不知道是否是批号所以反向归集来找
     *
     * @param cgdPhs 采购单批号集合
     * @param fpbz   发票备注
     * @return 匹配到的批号数组
     */
    public static Set<String> matchPhBycgdPhsAndFpbz(Set<String> cgdPhs, String fpbz) {
        Set<String> matches = new LinkedHashSet<>();
        if (StringUtils.isNotEmpty(fpbz)) {
            // 正则匹配字母数字和特殊符号组合
            Matcher matcher = Pattern.compile("[\\w&+-]+").matcher(fpbz);
            while (matcher.find()) {
                String candidate = matcher.group();
                if (cgdPhs.contains(candidate)) matches.add(candidate);
            }
        }
        return matches;
    }

    /**
     * 匹配商品名称
     *
     * @param fpHandledSpmc  发票商品名称
     * @param fpSpmc         发票商品原商品名称
     * @param cgdHandledSpmc 采购单商品名称
     * @return 是否匹配
     */
    public static boolean matchSpmc(String fpHandledSpmc, String fpSpmc, String cgdHandledSpmc) {
        if (StringUtils.isBlank(fpHandledSpmc)
                || StringUtils.isBlank(cgdHandledSpmc)
                || StringUtils.isBlank(fpSpmc)) {
            return false;
        }
        return fpHandledSpmc.contains(cgdHandledSpmc)
                || cgdHandledSpmc.contains(fpHandledSpmc)
                || fpSpmc.contains(cgdHandledSpmc)
                || cgdHandledSpmc.contains(fpSpmc);
    }

    /**
     * 预编译剂量匹配正则
     */
    private static final Pattern MATCH_GGXH_DOSE_PATTERN =
            Pattern.compile("(\\d+(\\.\\d+)?)(mg|g)", Pattern.CASE_INSENSITIVE);

    /**
     * 匹配商品规格
     *
     * @param fpGgxh          发票商品规格（原始）
     * @param fpHandledGgxh   发票商品规格（清洗后）
     * @param cgdGgxh         采购单商品规格（原始）
     * @param cgdHandledGgxh  采购单商品规格（清洗后）
     */
    public static boolean matchGgxh(String fpGgxh,
                                    String fpHandledGgxh,
                                    String cgdGgxh,
                                    String cgdHandledGgxh) {

        if (StringUtils.isAnyBlank(fpGgxh, fpHandledGgxh, cgdGgxh, cgdHandledGgxh)) {
            return false;
        }

        // 1️⃣ 构造所有候选规格
        List<String> fpSpecs = buildSpecList(fpGgxh, fpHandledGgxh);
        List<String> cgdSpecs = buildSpecList(cgdGgxh, cgdHandledGgxh);

        // 2️⃣ 两两匹配
        for (String fp : fpSpecs) {
            for (String cgd : cgdSpecs) {
                if (matchSingle(fp, cgd)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 构造规格集合（支持 _ 分隔）
     */
    private static List<String> buildSpecList(String raw, String handled) {

        Set<String> result = new LinkedHashSet<>();

        addSplitSpec(result, raw);
        addSplitSpec(result, handled);

        return new ArrayList<>(result);
    }

    /**
     * 添加规格并处理 _
     */
    private static void addSplitSpec(Set<String> set, String value) {

        if (StringUtils.isBlank(value)) {
            return;
        }

        value = value.toLowerCase().trim();

        if (value.contains("_")) {
            String[] split = value.split("_");
            for (String s : split) {
                if (StringUtils.isNotBlank(s)) {
                    set.add(s.trim());
                }
            }
        } else {
            set.add(value);
        }
    }

    /**
     * 单规格匹配逻辑
     */
    private static boolean matchSingle(String a, String b) {

        if (StringUtils.isBlank(a) || StringUtils.isBlank(b)) {
            return false;
        }

        // 1️⃣ 字符串包含匹配
        if (a.contains(b) || b.contains(a)) {
            return true;
        }

        // 2️⃣ 数值规格匹配
        List<BigDecimal> aList = extractAllMg(a);
        List<BigDecimal> bList = extractAllMg(b);

        if (aList.isEmpty() || bList.isEmpty()) {
            return false;
        }

        return compareDoseList(aList, bList);
    }

    /**
     * 提取所有规格并统一转为 mg
     *
     * 支持：
     * 80mg
     * 0.15g
     * 80mg/12.5mg
     */
    private static List<BigDecimal> extractAllMg(String ggxh) {

        if (StringUtils.isBlank(ggxh)) {
            return Collections.emptyList();
        }

        String normalized = Normalizer.normalize(ggxh, Normalizer.Form.NFKC)
                .replaceAll("[（(].*?[)）]", "") // 去括号
                .toLowerCase();

        Matcher m = MATCH_GGXH_DOSE_PATTERN.matcher(normalized);

        List<BigDecimal> result = new ArrayList<>();

        while (m.find()) {
            BigDecimal num = new BigDecimal(m.group(1));
            String unit = m.group(3);

            if ("g".equals(unit)) {
                num = num.multiply(BigDecimal.valueOf(1000));
            }

            result.add(num.stripTrailingZeros());
        }

        return result;
    }

    /**
     * 剂量集合比较
     *
     * 支持：
     * 80mg == 0.08g
     * 80mg/12.5mg == 80mg/12.5mg
     */
    private static boolean compareDoseList(List<BigDecimal> a, List<BigDecimal> b) {

        if (a.size() != b.size()) {
            return false;
        }

        List<BigDecimal> aSorted = new ArrayList<>(a);
        List<BigDecimal> bSorted = new ArrayList<>(b);

        Collections.sort(aSorted);
        Collections.sort(bSorted);

        for (int i = 0; i < aSorted.size(); i++) {
            if (aSorted.get(i).compareTo(bSorted.get(i)) != 0) {
                return false;
            }
        }

        return true;
    }


    /**
     * 预编译商品名称正则表达式，提升性能
     */
    private static final Pattern STAR_PATTERN =
            Pattern.compile("\\*([^*]*)$");

    private static final Pattern PREFIX_BRACKET_PATTERN =
            Pattern.compile("^\\s*[（(][^（）()]*[）)]\\s*");

    private static final Pattern PERCENT_PATTERN =
            Pattern.compile("\\d+(\\.\\d+)?%");

    private static final Pattern DOSE_PATTERN =
            Pattern.compile("\\d+[a-zA-Z]+");

    private static final Pattern SUFFIX_BRACKET_PATTERN =
            Pattern.compile("([（(][^（）()]*[）)])+$");


    /**
     * 处理商品名称字段
     * 1.提取星号(*)后的部分作为商品名称
     * 2.去除剂量规格（如25mg）
     * 3.去掉括号及其后面的内容（无论是否有星号）
     * 4.将字符串标准化并转换为半角字符
     *
     * @param spmc 原始商品名称字符串
     * @return 处理后的商品名称字符串
     */
    public static String handleSpmc(String spmc) {

        if (spmc == null || spmc.isEmpty()) {
            return null;
        }

        String processed = spmc;

        // 1️⃣ 星号后部分
        Matcher matcher = STAR_PATTERN.matcher(processed);
        if (matcher.find()) {
            processed = matcher.group(1);
        }

        // 2️⃣ 删除开头括号
        processed = PREFIX_BRACKET_PATTERN.matcher(processed).replaceAll("");

        // 3️⃣ 删除百分比
        processed = PERCENT_PATTERN.matcher(processed).replaceAll("");

        // 4️⃣ 删除剂量规格
        processed = DOSE_PATTERN.matcher(processed).replaceAll("");

        // 5️⃣ 删除末尾括号
        processed = SUFFIX_BRACKET_PATTERN.matcher(processed).replaceAll("");

        processed = processed.trim();

        return Normalizer.normalize(processed, Normalizer.Form.NFKC);
    }


    /**
     * 预编译规格型号正则表达式，提升性能
     */
    private static final Pattern BRACKET_PATTERN =
            Pattern.compile("\\([^()]*\\)");

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile(
                    "(\\*\\s*\\d+\\s*(支|瓶|盒|袋|片|粒|板))" +
                            "|(\\d+\\s*(支|瓶|盒|袋|片|粒|板)\\s*/\\s*(支|瓶|盒|袋|片|粒|板))"
            );

    private static final Pattern GGXH_DOSE_PATTERN =
            Pattern.compile("(\\d+(\\.\\d+)?\\s*(mg|g|μg|ml|L))",
                    Pattern.CASE_INSENSITIVE);


    /**
     * 处理商品规格字段
     *
     * @param ggxh 原始商品规格字符串
     * @return 处理后的商品规格字符串
     */
    public static String handleGgxh(String ggxh) {

        if (ggxh == null || ggxh.isBlank()) {
            return null;
        }

        // 1️⃣ 全角转半角（建议仅初始化阶段调用）
        String normalized = Normalizer.normalize(ggxh, Normalizer.Form.NFKC);

        // 2️⃣ 统一括号（用 replace 而不是 replaceAll）
        normalized = normalized.replace('（', '(')
                .replace('）', ')');

        // 3️⃣ 删除括号内容（预编译 Pattern）
        Matcher bracketMatcher = BRACKET_PATTERN.matcher(normalized);
        while (bracketMatcher.find()) {
            normalized = bracketMatcher.replaceAll("");
            bracketMatcher = BRACKET_PATTERN.matcher(normalized);
        }

        // 4️⃣ 单位统一（用 replace，不用正则）
        normalized = normalized
                .replace("毫克", "mg")
                .replace("克", "g")
                .replace("微克", "μg")
                .replace("毫升", "ml")
                .replace("升", "L");

        // 5️⃣ 去连接词（用 replace）
        normalized = normalized
                .replace("与", " ")
                .replace("和", " ");

        // 6️⃣ 删除包装说明（预编译 Pattern）
        normalized = PACKAGE_PATTERN.matcher(normalized)
                .replaceAll("");

        // 7️⃣ 提取所有剂量规格
        Matcher doseMatcher = GGXH_DOSE_PATTERN.matcher(normalized);

        StringBuilder sb = new StringBuilder();
        while (doseMatcher.find()) {
            sb.append(doseMatcher.group(1).replace(" ", ""));
        }

        if (!sb.isEmpty()) {
            return sb.toString();
        }

        return normalized.trim();
    }


    /**
     * 统一处理匹配成功逻辑
     * （发票 ↔ 采购单）
     * 一对多 或 一对一
     *
     * @param fp          发票
     * @param matchedCgds 命中的采购单列表
     * @param xsf         发票匹配数据
     * @param ctx         匹配上下文
     * @param strategy    匹配策略
     * @param matchStatus 匹配状态
     */
    public static void processMatchSuccess(Fp fp, List<Cgd> matchedCgds,
                                           XsfMatchData xsf,
                                           MatchContext ctx,
                                           StrategyEnum strategy,
                                           String matchStatus) {
        if (fp.isMatched()) {
            return;
        }
        if (matchedCgds == null || matchedCgds.isEmpty()) {
            return;
        }
        String fpInfo = String.format("%s_%s%s%s", fp.getXfmc(), fp.getFphm(), fp.getFpdm(), fp.getSdphm());

        Set<String> matchedDjbhs = matchedCgds.stream()
                .map(Cgd::getDjbh)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 标记采购单
        Iterator<Cgd> cgdIterator = xsf.getRemainingCgd().iterator();
        while (cgdIterator.hasNext()) {
            Cgd cgd = cgdIterator.next();
            if (matchedDjbhs.contains(cgd.getDjbh())) {
                log.info("匹配成功,策略：{} - 发票: {}, 采购单: {}", strategy.getstrategy(), fpInfo, cgd.getDjbh());

                cgd.markMatched(matchStatus);
                ctx.addMatched(fp, cgd, strategy.getstrategy(), matchStatus, null);

                // 从剩余池中移除，防止被后续策略或循环重复处理
                cgdIterator.remove();
            }
        }
        // 标记发票
        fp.markMatched(matchStatus);
    }

    /**
     * 统一处理匹配成功逻辑
     * （发票 ↔ 采购单）
     * 一对多 或 一对一
     *
     * @param fp          发票
     * @param matchedCgds 命中的采购单列表
     * @param xsf         发票匹配数据
     * @param ctx         匹配上下文
     * @param strategy    匹配策略
     * @param matchStatus 匹配状态
     */
    public static void processMatchSuccess(Fp fp,
                                           List<Cgd> matchedCgds,
                                           XsfMatchData xsf,
                                           MatchContext ctx,
                                           String strategy,
                                           String matchStatus
    ) {
        MatchUtils.processMatchWithWc(fp, matchedCgds, xsf, ctx, strategy, matchStatus, null);
    }


    public static void processMatchWithWc(Fp fp, List<Cgd> matchedCgds,
                                          XsfMatchData xsf,
                                          MatchContext ctx,
                                          String strategy,
                                          String matchStatus,
                                          String wcJe) {
        if (fp.isMatched()) {
            return;
        }
        if (matchedCgds == null || matchedCgds.isEmpty()) {
            return;
        }
        String fpInfo = String.format("%s_%s%s%s", fp.getXfmc(), fp.getFphm(), fp.getFpdm(), fp.getSdphm());

        Set<String> matchedDjbhs = matchedCgds.stream()
                .map(Cgd::getDjbh)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 标记采购单
        Iterator<Cgd> cgdIterator = xsf.getRemainingCgd().iterator();
        while (cgdIterator.hasNext()) {
            Cgd cgd = cgdIterator.next();
            if (matchedDjbhs.contains(cgd.getDjbh())) {
                log.info("匹配成功,策略：{} - 发票: {}, 采购单: {}", strategy, fpInfo, cgd.getDjbh());

                cgd.markMatched(matchStatus);
                ctx.addMatched(fp, cgd, strategy, matchStatus, wcJe);

                // 从剩余池中移除，防止被后续策略或循环重复处理
                cgdIterator.remove();
            }
        }
        // 标记发票
        fp.markMatched(matchStatus);
    }

    /**
     * 统一处理匹配成功逻辑
     * （采购单 ↔ 发票）
     * 一对多
     *
     * @param cgd         采购单
     * @param matchedFps  命中的发票列表
     * @param xsf         发票匹配数据
     * @param ctx         匹配上下文
     * @param strategy    匹配策略
     * @param matchStatus 匹配状态
     */
    public static void processMatchSuccess(Cgd cgd,
                                           List<Fp> matchedFps,
                                           XsfMatchData xsf,
                                           MatchContext ctx,
                                           String strategy,
                                           String matchStatus) {
        if (cgd.isMatched()) {
            return;
        }
        if (matchedFps == null || matchedFps.isEmpty()) {
            return;
        }
        // 标记采购单
        Iterator<Fp> fpIterator = xsf.getRemainingFp().iterator();
        while (fpIterator.hasNext()) {
            Fp fp = fpIterator.next();
            String fpInfo = String.format("%s_%s%s%s", fp.getXfmc(), fp.getFphm(), fp.getFpdm(), fp.getSdphm());
            // 发票号码相同
            if (matchedFps.stream().anyMatch(matchfp -> matchfp.getFphm().equals(fp.getFphm())
                    && matchfp.getFpdm().equals(fp.getFpdm())
                    && matchfp.getSdphm().equals(fp.getSdphm()))) {
                log.info("匹配成功,策略：{} - 发票: {}, 采购单: {}", strategy, fpInfo, cgd.getDjbh());

                cgd.markMatched(matchStatus);
                ctx.addMatched(fp, cgd, strategy, matchStatus, null);

                // 从剩余池中移除，防止被后续策略或循环重复处理
                fpIterator.remove();
            }
        }
        // 标记采购单
        cgd.markMatched(matchStatus);
    }


    /**
     * 匹配明细金额
     *
     * @param fpmxList  发票明细
     * @param cgdMxList 采购单明细
     * @return 匹配结果
     */
    public static boolean matchMxje(List<FpMx> fpmxList, List<CgdMx> cgdMxList) {
        return fpmxList.stream()
                .allMatch(fpMx -> cgdMxList.stream()
                        .anyMatch(cgdMx -> CalcUtils.compareJe(fpMx.getJshjBd(), cgdMx.getHsje())));
    }


    /**
     * 判断发票明细中金额是否一正一负
     */
    public static boolean isOnePositiveOneNegative(List<FpMx> fpMxs) {
        if (fpMxs == null || fpMxs.size() != 2) {
            return false;
        }
        return fpMxs.stream().anyMatch(fpMx -> fpMx.getJshjBd().compareTo(BigDecimal.ZERO) > 0)
                && fpMxs.stream().anyMatch(fpMx -> fpMx.getJshjBd().compareTo(BigDecimal.ZERO) < 0);

    }

    /**
     * 获取发票正数明细
     */
    public static FpMx getPositiveFpmx(List<FpMx> fpmxList) {
        return fpmxList.stream()
                .filter(fpMx -> fpMx.getJshjBd().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取发票负数明细
     */
    public static FpMx getNegativeFpmx(List<FpMx> fpmxList) {
        return fpmxList.stream()
                .filter(fpMx -> fpMx.getJshjBd().compareTo(BigDecimal.ZERO) < 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * 安全的字符串转整数
     *
     */
    public static Integer safeParseInt(String str) {
        if (str == null || str.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    /**
     * 抽取采购单商品规格
     */
    public static Set<String> extractSpggs(List<Cgd> cgds) {

        return cgds.stream()
                .filter(Objects::nonNull)
                .map(Cgd::getCgdMxList)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(CgdMx::getSpgg)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }


}
