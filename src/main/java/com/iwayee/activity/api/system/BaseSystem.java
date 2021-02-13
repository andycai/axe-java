package com.iwayee.activity.api.system;

import com.iwayee.activity.dao.mysql.DaoMgr;
import com.iwayee.activity.hub.CacheMgr;

public class BaseSystem {
  protected DaoMgr dao() {
    return DaoMgr.getInstance();
  }

  protected CacheMgr cache() {
    return CacheMgr.getInstance();
  }
}
