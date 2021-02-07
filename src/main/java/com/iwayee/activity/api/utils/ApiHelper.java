package com.iwayee.activity.api.utils;

import com.iwayee.activity.utils.StringUtils;

public class ApiHelper {
  public static String trimArrayChar(String s) {
    if (s != null && s != "") {
      var str = StringUtils.trim(s, "[");
      str = StringUtils.trim(str, "]");
      return str;
    }
    return "";
  }
}
