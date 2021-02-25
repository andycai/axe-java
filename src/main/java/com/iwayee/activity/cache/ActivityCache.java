package com.iwayee.activity.cache;

import com.iwayee.activity.api.comp.Activity;
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
public class ActivityCache extends BaseCache {
  private Map<Long, Activity> activities = new HashMap<>();
  private static final Logger LOG = LoggerFactory.getLogger(ActivityCache.class);

  public static ActivityCache getInstance() {
    return Singleton.instance(ActivityCache.class);
  }

  public void create(JsonObject jo, BiConsumer<Boolean, Long> action) {
    dao().act().create(jo, (b, newId) -> {
      if (b) {
        var activity = jo.mapTo(Activity.class);
        activity.id = newId;
        activities.put(activity.id, activity);
      }
      action.accept(b, newId);
    });
  }

  public void getActivityById(long id, BiConsumer<Boolean, Activity> action) {
    if (activities.containsKey(id)) {
      LOG.info("从缓存中获取活动数据：" + id);
      action.accept(true, activities.get(id));
    } else {
      LOG.info("从DB中获取活动数据：" + id);
      dao().act().getActivityById(id, (b, data) -> {
        Activity activity = null;
        if (b) {
          activity = data.mapTo(Activity.class);
          cache(activity);
        }
        action.accept(b, activity);
      });
    }
  }

  public void getActivitiesByType(int type, int status, int page, int num, BiConsumer<Boolean, JsonArray> action) {
    dao().act().getActivitiesByType(type, status, page, num, (b, data) -> {
      var jr = new JsonArray();
      if (b) {
        data.forEach(value -> {
          var jo = (JsonObject) value;
          var activity = jo.mapTo(Activity.class);
          // 缓存数据
          cache(activity);
          jr.add(activity);
        });
      }
      action.accept(b, jr);
    });
  }

  public void getActivitiesByIds(List<Long> ids, BiConsumer<Boolean, JsonArray> action) {
    var jr = new JsonArray();
    var idsForDB = new ArrayList<Long>();
    if (ids.isEmpty()) {
      action.accept(false, jr);
      return;
    }

    Iterator<Long> it = ids.iterator();
    while (it.hasNext()) {
      Long id = it.next();
      if (!jr.contains(id)) {
        if (activities.containsKey(id)) {
          jr.add(activities.get(id));
        } else {
          idsForDB.add(id);
        }
      }
    }

    if (idsForDB.size() > 0) {
      var idStr = joiner.join(idsForDB);
      LOG.info("从DB中获取活动数据：" + idStr);
      dao().act().getActivitiesByIds(idStr, (b, data) -> {
        if (b) {
          data.forEach(value -> {
            var jo = (JsonObject) value;
            var activity = jo.mapTo(Activity.class);
            // 缓存数据
            cache(activity);
            jr.add(activity);
          });
        }
        action.accept(b, jr);
      });
    } else {
      LOG.info("从缓存中获取活动数据：" + new JsonArray(ids).toString());
      action.accept(true, jr);
    }
  }

  public void syncToDB(long id, Consumer<Boolean> action) {
    if (activities.containsKey(id)) {
      var activity = activities.get(id);
      dao().act().updateActivityById(id, JsonObject.mapFrom(activity), b -> {
        action.accept(b);
      });
      return;
    }
    action.accept(false);
  }

  // 私有方法
  private void cache(Activity activity) {
    activities.put(activity.id, activity);
  }
}
