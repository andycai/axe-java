package com.iwayee.activity.cache;

import com.iwayee.activity.api.comp.Activity;
import com.iwayee.activity.utils.Singleton;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Consumer;

public class ActivityCache extends BaseCache {
  private Map<Integer, Activity> activities = new HashMap<>();

  public static ActivityCache getInstance() {
    return Singleton.instance(ActivityCache.class);
  }

  private void cache(Activity activity) {
    activities.put(activity.id, activity);
  }

  public void create(JsonObject jo, Consumer<Long> action) {
    dao().act().create(jo, newId -> {
      if (newId > 0L) {
        var activity = jo.mapTo(Activity.class);
        activity.id = newId.intValue();
        activities.put(activity.id, activity);
      }
      action.accept(newId);
    });
  }

  public void getActivityById(int id, Consumer<Activity> action) {
    if (activities.containsKey(id)) {
      System.out.println("从缓存中获取活动数据：" + id);
      action.accept(activities.get(id));
    } else {
      System.out.println("从DB中获取活动数据：" + id);
      dao().act().getActivityById(id, data -> {
        Activity activity = null;
        if (data != null) {
          activity = data.mapTo(Activity.class);
          cache(activity);
        }
        action.accept(activity);
      });
    }
  }

  public void getActivitiesByType(int type, int status, int page, int num, Consumer<JsonArray> action) {
    dao().act().getActivitiesByType(type, status, page, num, data -> {
      var jr = new JsonArray();
      if (!data.isEmpty()) {
        data.forEach(value -> {
          var jo = (JsonObject) value;
          var activity = jo.mapTo(Activity.class);
          // 缓存数据
          cache(activity);
          jr.add(activity);
        });
      }
      action.accept(jr);
    });
  }

  public void getActivitiesByIds(List<Integer> ids, Consumer<JsonArray> action) {
    var jr = new JsonArray();
    var idsForDB = new ArrayList<Integer>();
    if (ids.isEmpty()) {
      action.accept(jr);
      return;
    }

    Iterator<Integer> it = ids.iterator();
    while (it.hasNext()) {
      Integer id = it.next();
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
      System.out.println("从DB中获取活动数据：" + idStr);
      dao().act().getActivitiesByIds(idStr, data -> {
        if (data != null) {
          data.forEach(value -> {
            var jo = (JsonObject) value;
            var activity = jo.mapTo(Activity.class);
            // 缓存数据
            cache(activity);
            jr.add(activity);
          });
        }
        action.accept(jr);
      });
    } else {
      System.out.println("从缓存中获取活动数据：" + new JsonArray(ids).toString());
      action.accept(jr);
    }
  }

  public void syncToDB(int id, Consumer<Boolean> action) {
    if (activities.containsKey(id)) {
      var activity = activities.get(id);
      dao().act().updateActivityById(id, JsonObject.mapFrom(activity), b -> {
        action.accept(b);
      });
      return;
    }
    action.accept(false);
  }
}
