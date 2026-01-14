package com.current.rfyy.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zzy
 * @Date: 2026/1/9 15:16
 * @Description: TODO 拆分工具类
 **/
public class SplitUtils {

    public static <T> List<List<T>> split(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        if (list == null || list.isEmpty() || size <= 0) {
            return result;
        }
        int total = list.size();
        for (int i = 0; i < total; i += size) {
            result.add(list.subList(i, Math.min(i + size, total)));
        }
        return result;
    }
}
