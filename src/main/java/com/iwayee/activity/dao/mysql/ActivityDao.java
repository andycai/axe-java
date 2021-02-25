package com.iwayee.activity.dao.mysql;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ActivityDao extends MySQLDao {
  private static final Logger LOG = LoggerFactory.getLogger(ActivityDao.class);

  public void create(JsonObject act, BiConsumer<Boolean, Long> action) {
    String fields = "planner,group_id,kind,type,quota,title,`remark`,status,fee_type,fee_male,fee_female,queue,queue_sex,addr,ahead,begin_at,end_at";
    String sql = String.format("INSERT INTO activity (%s) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", fields);

    db().preparedQuery(sql)
            .execute(Tuple.of(
                    act.getInteger("planner"),
                    act.getInteger("group_id"),
                    act.getInteger("kind"),
                    act.getInteger("type"),
                    act.getInteger("quota"),
                    act.getString("title"),
                    act.getString("remark"),
                    act.getInteger("status"),
                    act.getInteger("fee_type"),
                    act.getInteger("fee_male"),
                    act.getInteger("fee_female"),
                    act.getString("queue"),
                    act.getString("queue_sex"),
                    act.getString("addr"),
                    act.getInteger("ahead"),
                    act.getString("begin_at"),
                    act.getString("end_at")))
            .onSuccess(rows -> {
              long lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID);
              LOG.info("Last Insert Id: " + lastInsertId);
              action.accept(lastInsertId > 0, lastInsertId);
            })
            .onFailure(th -> {
              LOG.info("Failure: " + th.getMessage());
              action.accept(false, 0L);
            });
  }

  public void getActivitiesByType(int type, int status, int page, int num, BiConsumer<Boolean, JsonArray> action) {
    String fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`,`queue_sex`,`addr`,`ahead`,`begin_at`,`end_at`";
    String sql = String.format("SELECT %s FROM `activity` WHERE `type` = ? AND `status` = ? ORDER BY id DESC LIMIT %d, %d", fields, (page - 1) * num, num);

    db().preparedQuery(sql)
            .execute(Tuple.of(type, status))
            .onSuccess(rows -> {
              JsonArray jr = new JsonArray();
              for (Row row : rows) {
                jr.add(toJo(row.toJson()));
              }
              action.accept(!jr.isEmpty(), jr);
            })
            .onFailure(th -> {
              LOG.info("Failure: " + th.getMessage());
              action.accept(false, new JsonArray());
            });
  }

  public void getActivityById(long id, BiConsumer<Boolean, JsonObject> action) {
    String fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`,`queue_sex`,`addr`,`ahead`,`begin_at`,`end_at`";
    String sql = String.format("SELECT %s FROM `activity` WHERE id=?", fields);

    db().preparedQuery(sql)
            .execute(Tuple.of(id))
            .onSuccess(rows -> {
              JsonObject jo = new JsonObject();
              for (Row row : rows) {
                jo = toJo(row.toJson());
              }
              action.accept(!jo.isEmpty(), jo);
            })
            .onFailure(th -> {
              LOG.info("Failure: " + th.getMessage());
              action.accept(false, new JsonObject());
            });
  }

  public void getActivitiesByIds(String ids, BiConsumer<Boolean, JsonArray> action) {
    String fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`,`queue_sex`,`addr`,`ahead`,`begin_at`,`end_at`";
    String sql = String.format("SELECT %s FROM activity WHERE id IN(%s)", fields, ids);

    db().preparedQuery(sql)
            .execute()
            .onSuccess(rows -> {
              JsonArray jr = new JsonArray();
              for (Row row : rows) {
                jr.add(toJo(row.toJson()));
              }
              action.accept(!jr.isEmpty(), jr);
            })
            .onFailure(th -> {
              LOG.info("Failure: " + th.getMessage());
              action.accept(false, new JsonArray());
            });
  }

  public void updateActivityStatus(long id, JsonObject act, Consumer<Boolean> action) {
    String fields = ""
            + "status = ?, "
            + "fee_male = ?, "
            + "fee_female = ?";
    String sql = String.format("UPDATE `activity` SET %s WHERE `id` = ?", fields);

    db().preparedQuery(sql)
            .execute(Tuple.of(
                    act.getInteger("status"),
                    act.getInteger("fee_male"),
                    act.getInteger("fee_female"),
                    id))
            .onSuccess(rows -> {
              action.accept(true);
            })
            .onFailure(th -> {
              LOG.info("Failure: " + th.getMessage());
              action.accept(false);
            });
  }

  public void updateActivityById(long id, JsonObject activity, Consumer<Boolean> action) {
    String fields = "quota = ?, "
            + "title = ?, "
            + "remark = ?, "
            + "status = ?, "
            + "ahead = ?, "
            + "queue = ?, "
            + "queue_sex = ?, "
            + "fee_male = ?, "
            + "fee_female = ?, "
            + "begin_at = ?, "
            + "end_at = ?, "
            + "addr = ?";
    String sql = String.format("UPDATE `activity` SET %s WHERE `id` = ?", fields);

    db().preparedQuery(sql)
            .execute(Tuple.of(
                    activity.getInteger("quota"),
                    activity.getString("title"),
                    activity.getString("remark"),
                    activity.getInteger("status"),
                    activity.getInteger("ahead"),
                    activity.getJsonArray("queue").encode(),
                    activity.getJsonArray("queue_sex").encode(),
                    activity.getInteger("fee_male"),
                    activity.getInteger("fee_female"),
                    activity.getString("begin_at"),
                    activity.getString("end_at"),
                    activity.getString("addr"),
                    id
            ))
            .onSuccess(rows -> {
              action.accept(true);
            })
            .onFailure(th -> {
              LOG.info("Failure: " + th.getMessage());
              action.accept(true);
            });
  }

  // 私有方法
  private JsonObject toJo(JsonObject jo) {
    jo.put("queue", new JsonArray(jo.getString("queue")));
    jo.put("queue_sex", new JsonArray(jo.getString("queue_sex")));
    return jo;
  }
}
