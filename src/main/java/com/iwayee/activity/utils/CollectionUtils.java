package com.iwayee.activity.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CollectionUtils {
  public static <T> List<T> subLastList(List<T> list, int page, int num) {
    var emptyList = new ArrayList<T>();
    if (Objects.isNull(list) || list.size() <= 0) return emptyList;

    final int offset = (page - 1) * num;
    int to = list.size() - offset;
    if (to <= 0) return emptyList;

    int from = to - num;
    if (from < 0) from = 0;

    var newList = list.subList(from, to);
    return newList;
  }
}
