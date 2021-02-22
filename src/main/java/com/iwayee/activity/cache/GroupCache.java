package com.iwayee.activity.cache;

import com.iwayee.activity.api.comp.Group;
import com.iwayee.activity.define.GroupPosition;
import com.iwayee.activity.func.Action;
import com.iwayee.activity.func.Action2;
import com.iwayee.activity.utils.Singleton;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

/**
 * TODO：缓存需要增加数量限制（LRU）
 */
public class GroupCache extends BaseCache {
  private Map<Integer, Group> groups = new HashMap<>();

  public static GroupCache getInstance() {
    return Singleton.instance(GroupCache.class);
  }

  private void cache(Group group) {
    if (group != null) {
      groups.put(group.id, group);
    }
  }

  public void create(JsonObject jo, long uid, Action2<Boolean, Long> action) {
    var group = jo.mapTo(Group.class);
    var now = new Date().getTime();
    group.level = 1;
    group.notice = "";
    var member = new JsonObject();
    member.put("id", uid);
    member.put("pos", GroupPosition.POS_OWNER.ordinal());
    member.put("at", now);
    group.members = new JsonArray().add(member);
    group.pending = new ArrayList<>();
    group.activities = new ArrayList<>();

    dao().group().create(JsonObject.mapFrom(group), (b, newId) -> {
      if (b) {
        group.id = newId.intValue();
        cache(group);
      }
      action.run(b, newId);
    });
  }

  public void getGroupById(int id, Action2<Boolean, Group> action) {
    if (groups.containsKey(id)) {
      System.out.println("从缓存中获取群组数据：" + id);
      action.run(true, groups.get(id));
    } else {
      System.out.println("从DB中获取群组数据：" + id);
      dao().group().getGroupByID(id, (b, data) -> {
        Group group = null;
        if (b) {
          group = data.mapTo(Group.class);
          cache(group);
        }
        action.run(b, group);
      });
    }
  }

  public void getGroupsByIds(List<Integer> ids, Action2<Boolean, JsonArray> action) {
    if (ids.size() <= 0) {
      action.run(false, null);
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
      dao().group().getGroupsByIds(idStr, (b, data) -> {
        if (b) {
          data.forEach(value -> {
            var jo = (JsonObject) value;
            var group = jo.mapTo(Group.class);
            cache(group);
            groupsMap.put(group.id, group);
            jr.add(group.toJson());
          });
        }
        action.run(b, jr);
      });
    } else {
      action.run(true, jr);
    }
  }

  public void getGroups(int page, int num, Action2<Boolean, JsonArray> action) {
    dao().group().getGroups(page, num, (b, data) -> {
      var jr = new JsonArray();
      if (b) {
        for (var g : data) {
          var group = ((JsonObject) g).mapTo(Group.class);
          cache(group);
          groups.put(group.id, group);

          var jo = group.toJson();
          jr.add(jo);
        }
      }

      action.run(b, jr);
    });
  }

  public void syncToDB(int id, Action<Boolean> action) {
    if (groups.containsKey(id)) {
      var group = groups.get(id);
      dao().group().updateGroupById(id, JsonObject.mapFrom(group), b -> {
        action.run(b);
      });
      return;
    }
    action.run(false);
  }
}
