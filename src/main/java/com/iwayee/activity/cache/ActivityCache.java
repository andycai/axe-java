package com.iwayee.activity.cache;

import com.iwayee.activity.api.comp.Activity;
import com.iwayee.activity.func.Action;
import com.iwayee.activity.func.Action2;
import com.iwayee.activity.utils.Singleton;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * TODO：缓存需要增加数量限制（LRU）
 */
public class ActivityCache extends BaseCache {
  private Map<Long, Activity> activities = new HashMap<>();
  private static final Logger LOG = LoggerFactory.getLogger(ActivityCache.class);

  public static ActivityCache getInstance() {
    return Singleton.instance(ActivityCache.class);
  }

  public void create(JsonObject jo, Action2<Boolean, Long> action) {
    dao().act().create(jo, (b, newId) -> {
      if (b) {
        var activity = jo.mapTo(Activity.class);
        activity.id = newId;
        activities.put(activity.id, activity);
      }
      action.run(b, newId);
    });
  }

  public void getActivityById(long id, Action2<Boolean, Activity> action) {
    if (activities.containsKey(id)) {
      LOG.info("从缓存中获取活动数据：" + id);
      action.run(true, activities.get(id));
    } else {
      LOG.info("从DB中获取活动数据：" + id);
      dao().act().getActivityById(id, (b, data) -> {
        Activity activity = null;
        if (b) {
          activity = data.mapTo(Activity.class);
          cache(activity);
        }
        action.run(b, activity);
      });
    }
  }

  public void getActivitiesByType(int type, int status, int page, int num, Action2<Boolean, JsonArray> action) {
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
      action.run(b, jr);
    });
  }

  public void getActivitiesByIds(List<Long> ids, Action2<Boolean, JsonArray> action) {
    var jr = new JsonArray();
    var idsForDB = new ArrayList<Long>();
    if (ids.isEmpty()) {
      action.run(false, jr);
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
        action.run(b, jr);
      });
    } else {
      LOG.info("从缓存中获取活动数据：" + new JsonArray(ids).toString());
      action.run(true, jr);
    }
  }

  public void syncToDB(long id, Action<Boolean> action) {
    if (activities.containsKey(id)) {
      var activity = activities.get(id);
      dao().act().updateActivityById(id, JsonObject.mapFrom(activity), b -> {
        action.run(b);
      });
      return;
    }
    action.run(false);
  }

  // 私有方法
  private void cache(Activity activity) {
    activities.put(activity.id, activity);
  }
}
