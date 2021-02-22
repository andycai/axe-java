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
  // TODO: 需要增加 page 和 num
  public void getActivitiesByUserId(Some some) {
    final var uid = some.userId();
    // 用户数据
    userCache().getUserById(uid, (ok, user) -> {
      if (!ok) {
        some.err(ErrCode.ERR_DATA);
        return;
      }
      if (user.activities.size() <= 0) {
        some.ok(new JsonArray());
        return;
      }
      // 活动数据
      actCache().getActivitiesByIds(user.activities, (ok2, acts) -> {
        var jr = new JsonArray();
        acts.forEach(value -> {
          jr.add(((Activity) value).toJson());
        });
        some.ok(jr);
      });
    });
  }

  // 根据群组获取活动
  // TODO: 需要增加 page 和 num
  public void getActivitiesByGroupId(Some some) {
    final var gid = some.getUInt("gid");

    // 群组数据
    cache().group().getGroupById(gid, (ok, data) -> {
      if (!ok) {
        some.err(ErrCode.ERR_DATA);
        return;
      }
      if (data.activities.size() <= 0) {
        some.ok(new JsonArray());
        return;
      }
      // 活动数据
      actCache().getActivitiesByIds(data.activities, (ok2, acts) -> {
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
    final var type = some.jsonUInt("type");
    final var status = some.jsonUInt("status");
    final var page = some.jsonUInt("page");
    final var num = some.jsonUInt("num");

    actCache().getActivitiesByType(type, status, page, num, (b, acts) -> {
      var jr = new JsonArray();
      acts.forEach(value -> {
        jr.add(((Activity) value).toJson());
      });
      some.ok(jr);
    });
  }

  // 获得单个活动数据
  public void getActivityById(Some some) {
    final var aid = some.getULong("aid");

    actCache().getActivityById(aid, (ok, activity) -> {
      if (!ok) {
        some.err(ErrCode.ERR_DATA);
      } else {
        userCache().getUsersByIds(activity.queue, (ok2, users) -> {
          var players = new JsonObject();
          if (ok2) {
            players = userCache().toPlayer(users);
          }
          var ret = JsonObject.mapFrom(activity);
          ret.put("players", players);
          some.ok(ret);
        });
      }
    });
  }

  private void doCreate(Some some, JsonObject jo, long uid, Group group) {
    actCache().create(jo, (ok, newId) -> {
      if (ok) {
        userCache().getUserById(uid, (ok2, user) -> {
          if (ok2) {
            // 用户活动列表更新
            user.addActivity(newId);
            userCache().syncToDB(uid, b -> {
              if (b) {
                // 群组活动列表更新
                if (group != null) {
                  group.addActivity(newId);
                  cache().group().syncToDB(group.id, b2 -> {
                    if (b2) {
                      some.ok((new JsonObject()).put("activity_id", newId));
                      return;
                    }
                    some.err(ErrCode.ERR_ACTIVITY_CREATE);
                  });
                  return;
                }
                some.ok((new JsonObject()).put("activity_id", newId));
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

    jo.put("queue", String.format("[%Ld]", uid));
    jo.put("queue_sex", String.format("[%d]", some.userSex()));
    jo.put("status", ActivityStatus.DOING.ordinal());

    // 群活动必须要群管理员才能创建
    var gid = some.jsonInt("group_id");
    if (gid > 0) {
      cache().group().getGroupById(gid, (ok, group) -> {
        if (!ok) {
          some.err(ErrCode.ERR_GROUP_GET_DATA);
          return;
        }
        if (!group.isManager(uid)) {
          some.err(ErrCode.ERR_GROUP_NON_MANAGER);
          return;
        }
        doCreate(some, jo, uid, group);
      });
      return;
    }
    doCreate(some, jo, uid, null);
  }

  private void doUpdate(Some some, Activity activity) {
    actCache().syncToDB(activity.id, b -> {
      if (!b) {
        some.err(ErrCode.ERR_ACTIVITY_UPDATE);
        return;
      }
      some.succeed();
    });
  }

  public void update(Some some) {
    final var aid = some.getULong("aid");
    final var quota = some.jsonUInt("quota");
    final var ahead = some.jsonUInt("ahead");
    final var fee_male = some.jsonInt("fee_male");
    final var fee_female = some.jsonInt("fee_female");
    final var title = some.jsonStr("title");
    final var remark = some.jsonStr("remark");
    final var addr = some.jsonStr("addr");
    final var begin_at = some.jsonStr("begin_at");
    final var end_at = some.jsonStr("end_at");
    final var uid = some.userId();

    ActivityCache.getInstance().getActivityById(aid, (ok, activity) -> {
      if (!ok) {
        some.err(ErrCode.ERR_ACTIVITY_GET_DATA);
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

      final var gid = activity.group_id;
      if (activity.inGroup()) { // 群组活动
        cache().group().getGroupById(gid, (ok2, group) -> {
          if (!ok2) {
            some.err(ErrCode.ERR_GROUP_GET_DATA);
          } else if (!group.isManager(uid)) {
            some.err(ErrCode.ERR_GROUP_NON_MANAGER);
          } else {
            doUpdate(some, activity);
          }
        });
      } else { // 个人发起活动
        if (activity.isPlanner(uid)) {
          doUpdate(some, activity);
        } else {
          some.err(ErrCode.ERR_ACTIVITY_NON_PLANNER);
        }
      }
    });
  }

  private void doEnd(Some some, int fee, long aid, Activity act) {
    // 结算或者终止
    act.settle(fee);
    var jo = new JsonObject();
    jo.put("status", act.status)
            .put("fee_male", act.fee_male)
            .put("fee_female", act.fee_female);
    dao().act().updateActivityStatus(aid, jo, ok -> {
      if (ok) {
        some.succeed();
      } else {
        some.err(ErrCode.ERR_OP);
      }
    });
  }

  // 结算活动
  public void end(Some some) {
    final var aid = some.getULong("aid");
    final var fee = some.jsonInt("fee"); // 单位：分
    final var uid = some.userId();

    actCache().getActivityById(aid, (ok, activity) -> {
      if (!ok) {
        some.err(ErrCode.ERR_ACTIVITY_GET_DATA);
      } else if (activity.inGroup()) {
        cache().group().getGroupById(activity.group_id, (ok2, group) -> {
          if (!ok2) {
            some.err(ErrCode.ERR_GROUP_GET_DATA);
          } else if (!group.isManager(uid)) {
            some.err(ErrCode.ERR_GROUP_NON_MANAGER);
          } else {
            doEnd(some, fee, aid, activity);
          }
        });
      } else {
        if (activity.isPlanner(uid)) {
          doEnd(some, fee, aid, activity);
        } else {
          some.err(ErrCode.ERR_ACTIVITY_NON_PLANNER);
        }
      }
    });
  }

  private void enqueue(Some some, long uid, Activity activity, int maleCount, int femaleCount) {
    activity.enqueue(uid, maleCount, femaleCount);
    actCache().syncToDB(activity.id, ok -> {
      if (ok) {
        some.succeed();
      } else {
        some.err(ErrCode.ERR_ACTIVITY_UPDATE);
      }
    });
  }

  /**
   * 报名，支持带多人报名
   */
  public void apply(Some some) {
    final var aid = some.getULong("aid");
    final var uid = some.userId();
    final var maleCount = some.jsonInt("male_count");
    final var femaleCount = some.jsonInt("female_count");

    ActivityCache.getInstance().getActivityById(aid, (ok, activity) -> {
      if (!ok) {
        some.err(ErrCode.ERR_ACTIVITY_GET_DATA);
      } else if (activity.overQuota(maleCount + femaleCount)) { // 候补数量不能超过10人
        some.err(ErrCode.ERR_ACTIVITY_OVER_QUOTA);
      } else if (activity.inGroup()) { // 必须是群组成员
        cache().group().getGroupById(activity.group_id, (ok2, group) -> {
          if (!ok2) {
            some.err(ErrCode.ERR_GROUP_GET_DATA);
          } else if (!group.isMember(uid)) {
            some.err(ErrCode.ERR_GROUP_NON_MEMBER);
          } else {
            enqueue(some, uid, activity, maleCount, femaleCount);
          }
        });
      } else {
        enqueue(some, uid, activity, maleCount, femaleCount);
      }
    });
  }

  private void dequeue(Some some, long uid, Activity activity, int maleCount, int femaleCount) {
    activity.dequeue(uid, maleCount, femaleCount);
    actCache().syncToDB(activity.id, ok -> {
      if (ok) {
        some.succeed();
      } else {
        some.err(ErrCode.ERR_ACTIVITY_UPDATE);
      }
    });
  }

  private void dequeue(Some some, Activity activity, int index) {
    if (activity.dequeue(index)) {
      actCache().syncToDB(activity.id, ok -> {
        if (ok) {
          some.succeed();
        } else {
          some.err(ErrCode.ERR_ACTIVITY_UPDATE);
        }
      });
    } else {
      some.err(ErrCode.ERR_ACTIVITY_REMOVE);
    }
  }

  /**
   * 取消报名，支持取消自带的多人
   */
  public void cancel(Some some) {
    final var aid = some.getULong("aid");
    final var uid = some.userId();
    final var maleCount = some.jsonInt("male_count");
    final var femaleCount = some.jsonInt("female_count");

    if (maleCount + femaleCount <= 0) {
      some.err(ErrCode.ERR_PARAM);
      return;
    }

    ActivityCache.getInstance().getActivityById(aid, (ok, activity) -> {
      if (!ok) {
        some.err(ErrCode.ERR_ACTIVITY_GET_DATA);
      } else if (activity.notEnough(uid, (maleCount + femaleCount))) { // 取消报名数量不正确
        some.err(ErrCode.ERR_ACTIVITY_NOT_ENOUGH);
      } else if (activity.inGroup()) {
        cache().group().getGroupById(activity.group_id, (ok2, group) -> {
          if (!ok2) {
            some.err(ErrCode.ERR_GROUP_GET_DATA);
          } else if (!group.isMember(uid)) {
            some.err(ErrCode.ERR_GROUP_NON_MEMBER);
          } else {
            dequeue(some, uid, activity, maleCount, femaleCount);
          }
        });
      } else {
        dequeue(some, uid, activity, maleCount, femaleCount);
      }
    });
  }

  // 移除报名队列中的人
  public void remove(Some some) {
    final var aid = some.getULong("aid");
    final var index = some.getUInt("index");
    final var uid = some.userId();

    ActivityCache.getInstance().getActivityById(aid, (ok, activity) -> {
      if (!ok) {
        some.err(ErrCode.ERR_ACTIVITY_GET_DATA);
      } else if (activity.inGroup()) {
        cache().group().getGroupById(activity.group_id, (ok2, group) -> {
          if (!ok2) {
            some.err(ErrCode.ERR_GROUP_GET_DATA);
          } else if (!group.isManager(uid)) { // 管理员才能移除
            some.err(ErrCode.ERR_GROUP_NON_MANAGER);
          } else {
            dequeue(some, activity, index);
          }
        });
      } else {
        // 发起人才能移除
        if (activity.isPlanner(uid)) {
          dequeue(some, activity, index);
        } else {
          some.err(ErrCode.ERR_ACTIVITY_NON_PLANNER);
        }
      }
    });
  }
}
