package com.iwayee.activity.api.system;

import com.iwayee.activity.cache.ActivityCache;
import com.iwayee.activity.cache.GroupCache;
import com.iwayee.activity.cache.UserCache;
import com.iwayee.activity.dao.mysql.ActivityDao;
import com.iwayee.activity.dao.mysql.DaoMgr;
import com.iwayee.activity.dao.mysql.GroupDao;
import com.iwayee.activity.dao.mysql.UserDao;
import com.iwayee.activity.hub.CacheMgr;

public class BaseSystem {
  protected DaoMgr dao() {
    return DaoMgr.getInstance();
  }

  protected UserDao userDao() {
    return dao().user();
  }

  protected GroupDao groupDao() {
    return dao().group();
  }

  protected ActivityDao actDao() {
    return dao().act();
  }

  protected CacheMgr cache() {
    return CacheMgr.getInstance();
  }

  protected UserCache userCache() {
    return cache().user();
  }

  protected GroupCache groupCache() {
    return cache().group();
  }

  protected ActivityCache actCache() {
    return cache().act();
  }
}
