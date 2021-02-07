package com.iwayee.activity.hub;

import com.iwayee.activity.api.comp.Group;
import com.iwayee.activity.define.GroupPosition;
import com.iwayee.activity.utils.Singleton;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Consumer;

public class GroupCache extends BaseCache {
  private Map<Integer, Group> groups = new HashMap<>();

  public static GroupCache getInstance() {
    return Singleton.instance(GroupCache.class);
  }

  private void cacheGroup(Group group) {
    if (group != null) {
      groups.put(group.id, group);
    }
  }

  public void create(JsonObject param, int uid, Consumer<Long> action) {
    var group = param.mapTo(Group.class);
    var now = new Date().getTime();
    group.level = 1;
    group.notice = "";
    var member = new JsonObject();
    member.put("id", uid);
    member.put("pos", GroupPosition.POS_OWNER.ordinal());
    member.put("at", now);
    group.members = new JsonArray().add(member);
    group.pending = new JsonArray();
    group.activities = new JsonArray();

    dao().group().create(JsonObject.mapFrom(group), lastInsertId -> {
      cacheGroup(group);
      action.accept(lastInsertId);
    });
  }

  public void getGroupById(int id, Consumer<Group> action) {
    if (groups.containsKey(id)) {
      System.out.println("从缓存中获取群组数据：" + id);
      action.accept(groups.get(id));
    } else {
      System.out.println("从DB中获取群组数据：" + id);
      dao().group().getGroupByID(id, data -> {
        Group group = null;
        if (data != null) {
          group = data.mapTo(Group.class);
          cacheGroup(group);
        }
        action.accept(group);
      });
    }
  }

  public void getGroupsByIds(List<Integer> ids, Consumer<JsonArray> action) {
    if (ids.size() <= 0) {
      action.accept(null);
      return;
    }
    var idsForDB = new ArrayList<Integer>();
    var groupsMap = new HashMap<Integer, Group>();
    var jr = new JsonArray();
    ids.forEach(id -> {
      if (!groupsMap.containsKey(id)) {
        if (groups.containsKey(id)) {
          var group = groups.get(id);
          groupsMap.put(id, group);
          jr.add(group.toJson());
        } else {
          idsForDB.add(id);
        }
      }
    });

    if (idsForDB.size() > 0) {
      String idStr = joiner.join(idsForDB);
      System.out.println("从DB中获取群组数据：" + idStr);
      dao().group().getGroupsByIds(idStr, data -> {
        if (data != null) {
          data.forEach(value -> {
            var jo = (JsonObject) value;
            var group = jo.mapTo(Group.class);
            cacheGroup(group);
            groupsMap.put(group.id, group);
            jr.add(group.toJson());
          });
        }
        action.accept(jr);
      });
    } else {
      action.accept(jr);
    }
  }

  public void getGroups(int page, int num, Consumer<JsonArray> action) {
    dao().group().getGroups(page, num, data -> {
      if (data == null) {
        action.accept(null);
      } else {
        // 先取出所有群组成员总和
        var ids = new ArrayList<Integer>(); // 这里没做去重处理，获取用户列表时会做
        for (var g : data) {
          var group = ((JsonObject)g).mapTo(Group.class);
          for (var m : group.members) {
            ids.add(((JsonObject)m).getInteger("id"));
          }
        }

        cache().user().getUsersByIds(ids, users -> {
          var jr = new JsonArray();
          for (var g : data) {
            var group = ((JsonObject)g).mapTo(Group.class);
            cacheGroup(group);
            groups.put(group.id, group);

            var jo = group.toJson();
//            var jo = JsonObject.mapFrom(group);
//            var members = cache().user().toMember(users, group.members);
//            jo.put("members", members);
            jr.add(jo);
          }
          action.accept(jr);
        });
      }
    });
  }

  public void syncToDB(int id, Consumer<Boolean> action) {
    if (groups.containsKey(id)) {
      var group = groups.get(id);
      dao().group().updateGroupById(id, JsonObject.mapFrom(group), b -> {
        action.accept(b);
      });
      return;
    }
    action.accept(false);
  }
}
