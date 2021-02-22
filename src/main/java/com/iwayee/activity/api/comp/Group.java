package com.iwayee.activity.api.comp;

import com.iwayee.activity.define.GroupPosition;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

final public class Group {
  public int id;
  public int level;
  public int scores;
  public String name;
  public String logo;
  public String notice;
  public String addr;
  public List<Long> activities;
  public JsonArray members;
  public List<Long> pending; // 申请入群列表

  public JsonObject toJson() {
    var jo = new JsonObject();
    jo.put("id", id);
    jo.put("level", level);
    jo.put("logo", logo);
    jo.put("name", name);
    jo.put("count", members.size());
    return jo;
  }

  public boolean notInPending(int index) {
    return index < 0 || index >= pending.size();
  }

  // 是否群成员
  public boolean isMember(long uid) {
    for (var item : members) {
      var jo = (JsonObject) item;
      if (jo.getLong("id") == uid) {
        return true;
      }
    }
    return false;
  }

  // 是否群主
  public boolean isOwner(long uid) {
    for (var item : members) {
      var jo = (JsonObject) item;
      if (jo.getLong("id") == uid
              && jo.getInteger("pos") == GroupPosition.POS_OWNER.ordinal()) {
        return true;
      }
    }
    return false;
  }

  // 是否群管理员
  public boolean isManager(long uid) {
    for (var item : members) {
      var jo = (JsonObject) item;
      if (jo.getLong("id") == uid
              && jo.getInteger("pos") > GroupPosition.POS_MEMBER.ordinal()) {
        return true;
      }
    }
    return false;
  }

  public int managerCount() {
    var count = 0;
    for (var item : members) {
      var jo = (JsonObject)item;
      if (jo.getInteger("pos") > GroupPosition.POS_MEMBER.ordinal()) {
        count += 1;
      }
    }
    return count;
  }

  public void addActivity(long aid) {
    if (!activities.contains(aid)) {
      activities.add(aid);
    }
  }

  public boolean promote(long uid) {
    for (var item : members) {
      var jo = (JsonObject) item;
      if (jo.getLong("id") == uid) {
        jo.put("pos", GroupPosition.POS_MANAGER.ordinal());
        return true;
      }
    }
    return false;
  }

  public boolean transfer(long uid, long mid) {
    var b = false;
    for (var item : members) {
      var jo = (JsonObject) item;
      // 外部自行判断权限
      if (jo.getLong("id") == uid) {
        jo.put("pos", GroupPosition.POS_MEMBER.ordinal());
      }
      if (jo.getLong("id") == mid) {
        jo.put("pos", GroupPosition.POS_OWNER.ordinal());
        b = true;
      }
    }
    return b;
  }

  public boolean remove(long mid) {
    var it = members.iterator();
    while (it.hasNext()) {
      var jo = (JsonObject) it.next();
      // 外部自行判断权限
      if (jo.getLong("id") == mid) {
        it.remove();
        return true;
      }
    }
    return false;
  }

  public boolean notIn(long uid) {
    for (var item : members) {
      var jo = (JsonObject) item;
      if (jo.getLong("id") == uid) {
        return false;
      }
    }
    return true;
  }
}
