package com.iwayee.activity.cache;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.iwayee.activity.dao.mysql.DaoMgr;
import com.iwayee.activity.hub.CacheMgr;

public class BaseCache {
  protected static final Joiner joiner = Joiner.on(",").skipNulls();
  protected static final Splitter splitter = Splitter.on(",").trimResults().omitEmptyStrings();

  protected CacheMgr cache() {
    return CacheMgr.getInstance();
  }
  protected DaoMgr dao() {
    return DaoMgr.getInstance();
  }
}
