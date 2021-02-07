package com.iwayee.activity.api.comp;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
}
