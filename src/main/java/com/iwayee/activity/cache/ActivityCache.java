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

  private void cacheActivity(Activity activity) {
    activities.put(activity.id, activity);
  }

  public void createActivity(JsonObject jo, Consumer<Long> action) {
    dao().act().create(jo, data -> {
      if (data > 0) {
        var activity = jo.mapTo(Activity.class);
        activities.put(activity.id, activity);
      }
      action.accept(data);
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
          cacheActivity(activity);
        }
        action.accept(activity);
      });
    }
  }

  public void getActivitiesByType(int type, int status, int page, int num, Consumer<Map<Integer, Activity>> action) {
    dao().act().getActivitiesByType(type, status, page, num, data -> {
      var actMap = new HashMap<Integer, Activity>();
      if (data != null) {
        data.forEach(value -> {
          var jo = (JsonObject) value;
          var activity = jo.mapTo(Activity.class);
          // 缓存数据
          cacheActivity(activity);
          actMap.put(activity.id, activity);
        });
      }
      action.accept(actMap);
    });
  }

  public void getActivitiesByIds(List<Integer> ids, Consumer<Map<Integer, Activity>> action) {
    if (ids.size() <= 0) {
      action.accept(null);
      return;
    }
    var actMap = new HashMap<Integer, Activity>();
    var idsForDB = new ArrayList<Integer>();
    Iterator<Integer> it = ids.iterator();
    while (it.hasNext()) {
      Integer id = it.next();
      if (!actMap.containsKey(id)) {
        if (activities.containsKey(id)) {
          actMap.put(id, activities.get(id));
        } else {
          idsForDB.add(id);
        }
      }
    }

    if (idsForDB.size() > 0) {
      var idStr = joiner.join(idsForDB);
      System.out.println("从DB中获取活动数据：" + idStr);
      dao().act().getActivitiesByList(idStr, data -> {
        if (data != null) {
          data.forEach(value -> {
            var jo = (JsonObject) value;
            var activity = jo.mapTo(Activity.class);
            // 缓存数据
            cacheActivity(activity);
            actMap.put(activity.id, activity);
          });
        }
        action.accept(actMap);
      });
    } else {
      System.out.println("从缓存中获取活动数据：" + new JsonArray(ids).toString());
      action.accept(actMap);
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
