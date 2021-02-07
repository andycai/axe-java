package com.iwayee.activity.hub;

import com.iwayee.activity.utils.Singleton;

public class CacheMgr {
  public static CacheMgr getInstance() {
    return Singleton.instance(CacheMgr.class);
  }

  public UserCache user() {
    return UserCache.getInstance();
  }

  public GroupCache group() {
    return GroupCache.getInstance();
  }

  public ActivityCache act() {
    return ActivityCache.getInstance();
  }
}
