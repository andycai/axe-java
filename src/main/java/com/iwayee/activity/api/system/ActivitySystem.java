package com.iwayee.activity.api.system;

import com.iwayee.activity.api.comp.Activity;
import com.iwayee.activity.api.comp.Group;
import com.iwayee.activity.cache.ActivityCache;
import com.iwayee.activity.define.ActivityFeeType;
import com.iwayee.activity.define.ActivityStatus;
import com.iwayee.activity.define.ErrCode;
import com.iwayee.activity.hub.Some;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

// 根据用户获取活动
public class ActivitySystem extends BaseSystem {
  public void getActivitiesByUserId(Some some) {
    var uid = some.userId();
    // 用户数据
    cache().user().getUserById(uid, user -> {
      if (user == null) {
        some.err(ErrCode.ERR_DATA);
        return;
      }
      if (user.activities.size() <= 0) {
        some.ok(new JsonArray());
        return;
      }
      // 活动数据
      cache().act().getActivitiesByIds(user.activities.getList(), acts -> {
        var jr = new JsonArray();
        acts.forEach(value -> {
          jr.add(((Activity) value).toJson());
        });
        some.ok(jr);
      });
    });
  }

  // 根据群组获取活动
  public void getActivitiesByGroupId(Some some) {
    var gid = some.getUInt("gid");

    // 群组数据
    cache().group().getGroupById(gid, data -> {
      if (data == null) {
        some.err(ErrCode.ERR_DATA);
        return;
      }
      if (data.activities.size() <= 0) {
        some.ok(new JsonArray());
        return;
      }
      // 活动数据
      cache().act().getActivitiesByIds(data.activities.getList(), acts -> {
        var jr = new JsonArray();
        acts.forEach(value -> {
          jr.add(((Activity) value).toJson());
        });
        some.ok(jr);
      });
    });
  }

  // 根据类型获取活动
  public void getActivities(Some some) {
    var type = some.jsonUInt("type");
    var status = some.jsonUInt("status");
    var page = some.jsonUInt("page");
    var num = some.jsonUInt("num");

    cache().act().getActivitiesByType(type, status, page, num, acts -> {
      var jr = new JsonArray();
      acts.forEach(value -> {
        jr.add(((Activity) value).toJson());
      });
      some.ok(jr);
    });
  }

  // 获得单个活动数据
  public void getActivityById(Some some) {
    var aid = some.getUInt("aid");

    cache().act().getActivityById(aid, activity -> {
      if (activity == null) {
        some.err(ErrCode.ERR_DATA);
      } else {
        cache().user().getUsersByIds(activity.queue.getList(), users -> {
          if (users == null) {
            some.err(ErrCode.ERR_DATA);
            return;
          }
          var players = cache().user().toPlayer(users);
          var ret = JsonObject.mapFrom(activity);
          ret.put("players", players);
          some.ok(ret);
        });
      }
    });
  }

  private void doCreate(Some some, JsonObject jo, int uid, Group group) {
    cache().act().create(jo, lastInsertId -> {
      if (lastInsertId > 0) {
        cache().user().getUserById(uid, user -> {
          if (user != null) {
            // 用户活动列表更新
            user.addActivity(lastInsertId.intValue());
            cache().user().syncToDB(uid, b -> {
              if (b) {
                // 群组活动列表更新
                if (group != null) {
                  group.addActivity(lastInsertId.intValue());
                  cache().group().syncToDB(group.id, b2 -> {
                    if (b2) {
                      some.ok((new JsonObject()).put("activity_id", lastInsertId));
                      return;
                    }
                    some.err(ErrCode.ERR_ACTIVITY_CREATE);
                  });
                  return;
                }
                some.ok((new JsonObject()).put("activity_id", lastInsertId));
                return;
              }
              some.err(ErrCode.ERR_ACTIVITY_CREATE);
            });
            return;
          }
          some.err(ErrCode.ERR_ACTIVITY_CREATE);
        });
      } else {
        some.err(ErrCode.ERR_ACTIVITY_CREATE);
      }
    });
  }

  // 创建获得活动
  public void create(Some some) {
    var uid = some.userId(); // 通过 session 获取
    var jo = new JsonObject();
    jo.put("planner", uid);
    jo.put("group_id", some.jsonInt("group_id"));
    jo.put("kind", some.jsonUInt("kind"));
    jo.put("type", some.jsonUInt("type"));
    jo.put("quota", some.jsonUInt("quota"));
    jo.put("fee_type", some.jsonUInt("fee_type"));
    jo.put("fee_male", some.jsonInt("fee_male"));
    jo.put("fee_female", some.jsonInt("fee_female"));
    jo.put("ahead", some.jsonUInt("ahead"));
    jo.put("title", some.jsonStr("title"));
    jo.put("remark", some.jsonStr("remark"));
    jo.put("addr", some.jsonStr("addr"));
    jo.put("begin_at", some.jsonStr("begin_at"));
    jo.put("end_at", some.jsonStr("end_at"));

    // 活动前结算，必须填写费用
    if (some.jsonUInt("fee_type") == ActivityFeeType.FEE_TYPE_BEFORE.ordinal()
      && (some.jsonInt("fee_male") == 0 || some.jsonInt("fee_female") == 0)
    ) {
      some.err(ErrCode.ERR_ACTIVITY_FEE);
      return;
    }

    jo.put("queue", String.format("[%d]", uid));
    jo.put("queue_sex", String.format("[%d]", some.userSex()));
    jo.put("status", ActivityStatus.DOING.ordinal());

    // 群活动必须要群管理员才能创建
    var gid = some.jsonInt("group_id");
    if (gid > 0) {
      cache().group().getGroupById(gid, group -> {
        if (!group.isManager(uid)) {
          some.err(ErrCode.ERR_GROUP_NOT_MANAGER);
          return;
        }
        doCreate(some, jo, uid, group);
      });
      return;
    }
    doCreate(some, jo, uid, null);
  }

  private void doUpdate(Some some, Activity activity) {
    cache().act().syncToDB(activity.id, b -> {
      if (!b) {
        some.err(ErrCode.ERR_ACTIVITY_UPDATE);
        return;
      }
      some.succeed();
    });
  }

  public void update(Some some) {
    var aid = some.getUInt("aid");
    var quota = some.jsonUInt("quota");
    var ahead = some.jsonUInt("ahead");
    var fee_male = some.jsonInt("fee_male");
    var fee_female = some.jsonInt("fee_female");
    var title = some.jsonStr("title");
    var remark = some.jsonStr("remark");
    var addr = some.jsonStr("addr");
    var begin_at = some.jsonStr("begin_at");
    var end_at = some.jsonStr("end_at");
    var uid = some.userId();

    ActivityCache.getInstance().getActivityById(aid, activity -> {
      if (activity == null) {
        some.err(ErrCode.ERR_ACTIVITY_NO_DATA);
        return;
      }
      activity.quota = quota;
      activity.ahead = ahead;
      activity.fee_male = fee_male;
      activity.fee_female = fee_female;
      activity.title = title;
      activity.remark = remark;
      activity.addr = addr;
      activity.begin_at = begin_at;
      activity.end_at = end_at;

      // 群活动必须要群管理员才能更新
      var gid = activity.group_id;
      if (activity.inGroup()) {
        cache().group().getGroupById(gid, group -> {
          if (!group.isManager(uid)) {
            some.err(ErrCode.ERR_GROUP_NOT_MANAGER);
            return;
          }
          doUpdate(some, activity);
        });
        return;
      }

      if (activity.planner != uid) {
        some.err(ErrCode.ERR_ACTIVITY_NOT_PLANNER);
        return;
      }
      doUpdate(some, activity);
    });
  }

  private void doEnd(Some some, int aid, JsonObject jo) {
    dao().act().updateActivityStatus(aid, jo, b -> {
      if (!b) {
        some.err(ErrCode.ERR_OP);
        return;
      }
      some.succeed();
    });
  }

  // 结算活动
  public void end(Some some) {
    var aid = some.getUInt("aid");
    var status = some.jsonBool("end") ? ActivityStatus.END.ordinal() : ActivityStatus.DONE.ordinal();
    var uid = some.userId();
    var jo = new JsonObject();
    jo.put("status", status);
    jo.put("fee_male", some.jsonInt("fee_male"));
    jo.put("fee_female", some.jsonInt("fee_female"));

    cache().act().getActivityById(aid, activity -> {
      if (activity == null) {
        some.err(ErrCode.ERR_ACTIVITY_NO_DATA);
        return;
      }

      // 群活动
      if (activity.inGroup()) {
        cache().group().getGroupById(activity.group_id, group -> {
          if (!group.isManager(uid)) {
            some.err(ErrCode.ERR_GROUP_NOT_MANAGER);
            return;
          }
          doEnd(some, aid, jo);
        });
        return;
      }
      doEnd(some, aid, jo);
    });
  }

  private void enqueue(Some some, int uid, Activity activity, int maleCount, int femaleCount) {
    activity.enqueue(uid, maleCount, femaleCount);
    cache().act().syncToDB(activity.id, b -> {
      if (!b) {
        some.err(ErrCode.ERR_ACTIVITY_UPDATE);
        return;
      }
      some.succeed();
    });
  }

  /**
   * 报名，支持带多人报名
   */
  public void apply(Some some) {
    var aid = some.getUInt("aid");
    var uid = some.userId();
    var maleCount = some.jsonInt("male_count");
    var femaleCount = some.jsonInt("female_count");

    ActivityCache.getInstance().getActivityById(aid, activity -> {
      if (activity == null) {
        some.err(ErrCode.ERR_ACTIVITY_NO_DATA);
        return;
      }

      // 候补数量不能超过10人
      if (activity.overQuota(uid, (maleCount + femaleCount))) {
        some.err(ErrCode.ERR_ACTIVITY_OVER_QUOTA);
        return;
      }

      // 必须是群组成员
      if (activity.inGroup()) {
        cache().group().getGroupById(activity.group_id, group -> {
          if (!group.isMember(uid)) {
            some.err(ErrCode.ERR_ACTIVITY_CANNOT_APPLY_NOT_IN_GROUP);
            return;
          }
          enqueue(some, uid, activity, maleCount, femaleCount);
        });
        return;
      }
      enqueue(some, uid, activity, maleCount, femaleCount);
    });
  }

  private void dequeue(Some some, int uid, Activity activity, int maleCount, int femaleCount) {
    activity.dequeue(uid, maleCount, femaleCount);
    cache().act().syncToDB(activity.id, b -> {
      if (!b) {
        some.err(ErrCode.ERR_ACTIVITY_UPDATE);
        return;
      }
      some.succeed();
    });
  }

  /**
   * 取消报名，支持取消自带的多人
   */
  public void cancel(Some some) {
    var aid = some.getUInt("aid");
    var uid = some.userId();
    var maleCount = some.jsonInt("male_count");
    var femaleCount = some.jsonInt("female_count");

    ActivityCache.getInstance().getActivityById(aid, activity -> {
      if (activity == null) {
        some.err(ErrCode.ERR_ACTIVITY_NO_DATA);
        return;
      }

      if (activity.notEnough(uid, (maleCount + femaleCount))) {
        some.err(ErrCode.ERR_ACTIVITY_NOT_ENOUGH);
        return;
      }

      // 必须是群组成员
      if (activity.inGroup()) {
        cache().group().getGroupById(activity.group_id, group -> {
          if (!group.isMember(uid)) {
            some.err(ErrCode.ERR_ACTIVITY_CANNOT_APPLY_NOT_IN_GROUP);
            return;
          }
          dequeue(some, uid, activity, maleCount, femaleCount);
        });
        return;
      }
      dequeue(some, uid, activity, maleCount, femaleCount);
    });
  }
}
