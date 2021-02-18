package com.iwayee.activity.dao.mysql;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.function.Consumer;

public class ActivityDao extends MySQLDao {
  public void create(JsonObject act, Consumer<Long> action) {
    var fields = "planner,group_id,kind,type,quota,title,`remark`,status,fee_type,fee_male,fee_female,queue,queue_sex,addr,ahead,begin_at,end_at";
    var sql = String.format("INSERT INTO activity (%s) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", fields);

    db().preparedQuery(sql).execute(Tuple.of(
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
            act.getString("end_at")
    ), ar -> {
      var lastInsertId = 0L;
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID);
        System.out.println("Last Insert Id: " + lastInsertId);
      } else {
        System.out.println("Failure: " + ar.cause().getMessage());
      }
      action.accept(lastInsertId);
    });
  }

  public void getActivitiesByType(int type, int status, int page, int num, Consumer<JsonArray> action) {
    var fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`,`queue_sex`,`addr`,`ahead`,`begin_at`,`end_at`";
    var sql = String.format("SELECT %s FROM `activity` WHERE `type` = ? AND `status` = ? ORDER BY id DESC LIMIT %d, %d", fields, (page - 1) * num, num);

    db().preparedQuery(sql).execute(Tuple.of(type, status), ar -> {
      var jr = new JsonArray();
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        for (Row row : rows) {
          jr.add(row.toJson());
        }
      } else {
        System.out.println("Failure: " + ar.cause().getMessage());
      }
      action.accept(jr);
    });
  }

  public void getActivityById(long id, Consumer<JsonObject> action) {
    var fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`,`queue_sex`,`addr`,`ahead`,`begin_at`,`end_at`";
    var sql = String.format("SELECT %s FROM `activity` WHERE id=?", fields);

    db().preparedQuery(sql).execute(Tuple.of(id), ar -> {
      JsonObject jo = null;
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        for (Row row : rows) {
          jo = row.toJson();
        }
      } else {
        System.out.println("Failure: " + ar.cause().getMessage());
      }
      action.accept(jo);
    });
  }

  public void getActivitiesByIds(String ids, Consumer<JsonArray> action) {
    var fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`,`queue_sex`,`addr`,`ahead`,`begin_at`,`end_at`";
    var sql = String.format("SELECT %s FROM activity WHERE id IN(%s)", fields, ids);

    db().preparedQuery(sql).execute(ar -> {
      var jr = new JsonArray();
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        for (Row row : rows) {
          jr.add(row.toJson());
        }
      } else {
        System.out.println("Failure: " + ar.cause().getMessage());
      }
      action.accept(jr);
    });
  }

  public void updateActivityStatus(long id, JsonObject act, Consumer<Boolean> action) {
    var fields = ""
            + "status = ?, "
            + "fee_male = ?, "
            + "fee_female = ?";
    var sql = String.format("UPDATE `activity` SET %s WHERE `id` = ?", fields);

    db().preparedQuery(sql).execute(Tuple.of(
            act.getInteger("status"),
            act.getInteger("fee_male"),
            act.getInteger("fee_female"),
            id
    ), ar -> {
      var ret = false;
      if (ar.succeeded()) {
        ret = true;
      } else {
        System.out.println("Failure: " + ar.cause().getMessage());
      }
      action.accept(ret);
    });
  }

  public void updateActivityById(long id, JsonObject activity, Consumer<Boolean> action) {
    var fields = "quota = ?, "
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
    var sql = String.format("UPDATE `activity` SET %s WHERE `id` = ?", fields);

    db().preparedQuery(sql).execute(Tuple.of(
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
    ), ar -> {
      var ret = false;
      if (ar.succeeded()) {
        ret = true;
      } else {
        System.out.println("Failure: " + ar.cause().getMessage());
      }
      action.accept(ret);
    });
  }
}
