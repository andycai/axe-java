package com.iwayee.activity.api.comp;

import com.iwayee.activity.define.GroupPosition;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

final public class Group {
  public int id;
  public int level;
  public String name;
  public String logo;
  public String notice;
  public String addr;
  public JsonArray activities;
  public JsonArray members;
  public JsonArray pending; // 申请入群列表

  public JsonObject toJson() {
    var jo = new JsonObject();
    jo.put("id", id);
    jo.put("level", level);
    jo.put("logo", logo);
    jo.put("name", name);
    jo.put("count", members.size());
    return jo;
  }

  public boolean isMember(int uid) {
    for (var m : members) {
      var jo = (JsonObject)m;
      if (jo.getInteger("id") == uid) {
        return true;
      }
    }
    return false;
  }

  // 是否管理员
  public boolean isOwner(int uid) {
    for (var m : members) {
      var jo = (JsonObject)m;
      if (jo.getInteger("id") == uid
        && jo.getInteger("pos") == GroupPosition.POS_OWNER.ordinal()) {
        return true;
      }
    }
    return false;
  }

  // 是否管理员
  public boolean isManager(int uid) {
    for (var m : members) {
      var jo = (JsonObject)m;
      if (jo.getInteger("id") == uid
        && jo.getInteger("pos") > GroupPosition.POS_MEMBER.ordinal()) {
        return true;
      }
    }
    return false;
  }

  public int mCount() {
    var count = 0;
    for (var m : members) {
      var jo = (JsonObject)m;
      if (jo.getInteger("pos") > GroupPosition.POS_MEMBER.ordinal()) {
        count += 1;
      }
    }
    return count;
  }

  public void addActivity(int aid) {
    if (!activities.contains(aid)) {
      activities.add(aid);
    }
  }

  public boolean promote(int uid) {
    for (var m : members) {
      var jo = (JsonObject)m;
      if (jo.getInteger("id") == uid) {
        jo.put("pos", GroupPosition.POS_MANAGER.ordinal());
        return true;
      }
    }
    return false;
  }

  public boolean transfer(int uid, int mid) {
    var b = false;
    for (var m : members) {
      var jo = (JsonObject)m;
      // 外部自行判断权限
      if (jo.getInteger("id") == uid) {
        jo.put("pos", GroupPosition.POS_MEMBER.ordinal());
      }
      if (jo.getInteger("id") == mid) {
        jo.put("pos", GroupPosition.POS_OWNER.ordinal());
        b = true;
      }
    }
    return b;
  }

  public boolean notIn(int uid) {
    for (var m : members) {
      var jo = (JsonObject)m;
      if (jo.getInteger("id") == uid) {
        return false;
      }
    }
    return true;
  }
}
