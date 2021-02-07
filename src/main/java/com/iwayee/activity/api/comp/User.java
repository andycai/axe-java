package com.iwayee.activity.api.comp;

import io.vertx.core.json.JsonArray;

final public class User {
  public int id;
  public int sex;
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

  public JsonArray groups;
  public JsonArray activities;

  public void addActivity(int aid) {
    if (!activities.contains(aid)) {
      activities.add(aid);
    }
  }

  public void addGroup(int gid) {
    if (!groups.contains(gid)) {
      groups.add(gid);
    }
  }
}
