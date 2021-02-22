package com.iwayee.activity.api.comp;

import java.util.List;

final public class User {
  public long id;
  public int sex;
  public int scores;
  public String username;
  public String password;
  public String token; // 自定义
  public String wx_token; // 微信
  public String wx_nick; // 微信
  public String nick;
  public String ip;
  public String phone;
  public String email;
  public String create_at;

  public List<Integer> groups;
  public List<Long> activities;

  public boolean containsActivity(long aid) {
    return activities.contains(aid);
  }

  public boolean addActivity(long aid) {
    if (!activities.contains(aid)) {
      activities.add(aid);
      return true;
    }
    return false;
  }

  public boolean removeActivity(long aid) {
    if (activities.contains(aid)) {
      activities.remove(aid);
      return true;
    }
    return false;
  }

  public boolean addGroup(int gid) {
    if (!groups.contains(gid)) {
      groups.add(gid);
      return true;
    }
    return false;
  }

  public boolean removeGroup(int gid) {
    if (groups.contains(gid)) {
      groups.remove((Object) gid);
      return true;
    }
    return false;
  }
}
