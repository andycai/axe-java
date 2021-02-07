package com.iwayee.activity.api.comp;

import com.iwayee.activity.define.SexType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;

final public class Activity {
  public int id;
  public int planner;
  public int kind; // 活动分类:1羽毛球,2篮球,3足球,4聚餐...
  public int type; // 活动类型:1全局保护,2全局公开,3群组
  public int status; // 活动状态:1进行中,2正常结算完成,3手动终止
  public int quota; // 名额
  public int group_id; // 群组ID
  public int ahead; // 提前取消报名限制（小时）
  public int fee_type; // 结算方式:1免费,2活动前,3活动后男女平均,4活动后男固定|女平摊,5活动后男平摊|女固定
  public int fee_male; // 男费用
  public int fee_female; // 女费用
  public String title;
  public String remark;
  public String addr;
  public String begin_at;
  public String end_at;
  public JsonArray queue; // 报名队列
  public JsonArray queue_sex; // 报名队列中的性别

  public JsonObject toJson() {
    var jo = new JsonObject();
    jo.put("id", id);
    jo.put("status", status);
    jo.put("quota", quota);
    jo.put("count", queue.size());
    jo.put("title", title);
    jo.put("remark", remark);
    jo.put("begin_at", begin_at);
    jo.put("end_at", end_at);
    return jo;
  }

  public boolean isGroupActivity() {
    return group_id > 0;
  }

  // 报名的人数超过候补的限制，避免乱报名，如带100000人报名
  public boolean overQuota(int uid, int total) {
    return (queue.size() + total) - quota > 10;
  }

  // 要取消报名的数量超过已经报名的数量
  public boolean notEnough(int uid, int total) {
    var count = 0;
    for (var val : queue) {
      if ((int)val == uid) {
        count += 1;
      }
    }
    return total > count;
  }

  public void fixQueue() {
    var df = queue_sex.size() - queue.size();
    if (df > 0) {
      var size = queue_sex.size();
      for (int i = size - 1; i >= size - df; i--) {
        queue_sex.remove(i);
      }
    }
    if (df < 0) {
      var size = queue.size();
      for (int i = size - 1; i >= size + df; i--) {
        queue.remove(i);
      }
    }
  }

  public void enqueue(int uid, int maleCount, int femaleCount) {
    fixQueue();
    for (int i = 0; i < maleCount; i++) {
      queue.add(uid);
      queue_sex.add(SexType.SEX_TYPE_MALE.ordinal());
    }
    for (int i = 0; i < femaleCount; i++) {
      queue.add(uid);
      queue_sex.add(SexType.SEX_TYPE_FEMALE.ordinal());
    }
  }

  public void dequeue(int uid, int maleCount, int femaleCount) {
    fixQueue();
    var mCount = 0;
    var fCount = 0;
    var size = queue.size();
    var posArr = new ArrayList<Integer>();
    for (int i = size - 1; i >= 0; i--) {
      var val = queue.getInteger(i);
      if (val == uid) {
        // 男
        if (queue_sex.getInteger(i) == SexType.SEX_TYPE_MALE.ordinal() && maleCount > mCount) {
          mCount += 1;
          posArr.add(i);
        }
        // 女
        if (queue_sex.getInteger(i) == SexType.SEX_TYPE_FEMALE.ordinal() && femaleCount > fCount) {
          fCount += 1;
          posArr.add(i);
        }
        if (mCount >= maleCount && fCount >= femaleCount) {
          break;
        }
      }
    }
    var total = posArr.size();
    for (int i = 0; i < total; i++) {
      queue.remove((int)posArr.get(i));
      queue_sex.remove((int)posArr.get(i));
    }
  }
}
