package com.iwayee.activity.cache;

import com.iwayee.activity.api.comp.Group;
import com.iwayee.activity.define.GroupPosition;
import com.iwayee.activity.utils.Singleton;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * TODO：缓存需要增加数量限制（LRU）
 */
public class GroupCache extends BaseCache {
  private Map<Integer, Group> groups = new HashMap<>();
  private static final Logger LOG = LoggerFactory.getLogger(GroupCache.class);

  public static GroupCache getInstance() {
    return Singleton.instance(GroupCache.class);
  }

  public void create(JsonObject jo, long uid, BiConsumer<Boolean, Long> action) {
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
      action.accept(b, newId);
    });
  }

  public void getGroupById(int id, BiConsumer<Boolean, Group> action) {
    if (groups.containsKey(id)) {
      LOG.info("从缓存中获取群组数据：" + id);
      action.accept(true, groups.get(id));
    } else {
      LOG.info("从DB中获取群组数据：" + id);
      dao().group().getGroupByID(id, (b, data) -> {
        Group group = null;
        if (b) {
          group = data.mapTo(Group.class);
          cache(group);
        }
        action.accept(b, group);
      });
    }
  }

  public void getGroupsByIds(List<Integer> ids, BiConsumer<Boolean, JsonArray> action) {
    if (ids.size() <= 0) {
      action.accept(false, null);
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
      LOG.info("从DB中获取群组数据：" + idStr);
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
        action.accept(b, jr);
      });
    } else {
      action.accept(true, jr);
    }
  }

  public void getGroups(int page, int num, BiConsumer<Boolean, JsonArray> action) {
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

      action.accept(b, jr);
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

  // 私有方法
  private void cache(Group group) {
    if (group != null) {
      groups.put(group.id, group);
    }
  }
}
