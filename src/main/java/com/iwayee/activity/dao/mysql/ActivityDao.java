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
    conn().onSuccess(conn -> {
      conn
      .preparedQuery(sql)
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
      .onComplete(ar -> {
        if (ar.succeeded()) {
          RowSet<Row> rows = ar.result();
          long lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID);
          System.out.println("Last Insert Id: " + lastInsertId);
          action.accept(lastInsertId);
        } else {
          System.out.println("Failure: " + ar.cause().getMessage());
          action.accept(0L);
        }
        conn.close();
      }).onFailure(ar -> {
        action.accept(0L);
        System.out.println("Failure: " + ar.getMessage());
        ar.printStackTrace();
      });
    });
  }

  public void getActivitiesByType(int type, int status, int page, int num, Consumer<JsonArray> action) {
    var fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`,`addr`,`ahead`,`begin_at`,`end_at`";
    var sql = String.format("SELECT %s FROM `activity` WHERE `type` = ? AND `status` = ? ORDER BY ID DESC LIMIT %d, %d", fields, (page-1)*num, num);
    conn().onSuccess(conn -> {
      conn
        .preparedQuery(sql)
        .execute(Tuple.of(type, status))
        .onComplete(ar -> {
          if (ar.succeeded()) {
            var list = new JsonArray();
            RowSet<Row> rows = ar.result();
            for (Row row : rows) {
              var jo = row.toJson();
//              jo.put("queue", new JsonArray(jo.getString("queue")));
//              jo = JsonObject.mapFrom(jo.mapTo(Activity.class));
              list.add(jo);
            }
            action.accept(list);
          } else {
            System.out.println("Failure: " + ar.cause().getMessage());
            action.accept(new JsonArray());
          }
          conn.close();
        }).onFailure(ar -> {
          action.accept(new JsonArray());
          ar.printStackTrace();
      });
    });
  }

  public void getActivityById(int id, Consumer<JsonObject> action) {
    var fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`,`queue_sex`,`addr`,`ahead`,`begin_at`,`end_at`";
    var sql = String.format("SELECT %s FROM `activity` WHERE id=?", fields);
    conn().onSuccess(conn -> {
      conn
        .preparedQuery(sql)
        .execute(Tuple.of(id))
        .onComplete(ar -> {
          if (ar.succeeded()) {
            RowSet<Row> rows = ar.result();
            if (rows.size() <= 0) {
              action.accept(null);
            } else {
              JsonObject data = new JsonObject();
              for (Row row : rows) {
                data = row.toJson();
              }
              action.accept(data);
            }
          } else {
            System.out.println("Failure: " + ar.cause().getMessage());
            action.accept(null);
          }
          conn.close();
        }).onFailure(ar -> {
          action.accept(null);
        ar.printStackTrace();
      });
    });
  }

  public void getActivitiesByList(String ins, Consumer<JsonArray> action) {
    var fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`,`addr`,`ahead`,`begin_at`,`end_at`";
    var sql = String.format("SELECT %s FROM activity WHERE id IN(%s)", fields, ins);
    conn().onSuccess(conn -> {
      conn
        .preparedQuery(sql)
        .execute()
        .onComplete(ar -> {
          if (ar.succeeded()) {
            RowSet<Row> rows = ar.result();
            var list = new JsonArray();
            for (Row row : rows) {
              var jo = row.toJson();
              list.add(jo);
            }
            action.accept(list);
          } else {
            System.out.println("Failure: " + ar.cause().getMessage());
            action.accept(new JsonArray());
          }
          conn.close();
        }).onFailure(ar -> {
          action.accept(new JsonArray());
          ar.printStackTrace();
      });
    });
  }

  public void updateActivityStatus(int id, JsonObject jo, Consumer<Boolean> action) {
    var fields = ""
      + "status = ?, "
      + "fee_male = ?, "
      + "fee_female = ?"
      ;
    var sql = String.format("UPDATE `activity` SET %s WHERE `id` = ?", fields);
    conn().onSuccess(conn -> {
      conn
        .preparedQuery(sql)
        .execute(Tuple.of(
          jo.getInteger("status"),
          jo.getInteger("fee_male"),
          jo.getInteger("fee_female"),
          id
        ))
        .onComplete(ar -> {
          if (ar.succeeded()) {
            action.accept(true);
          } else {
            System.out.println("Failure: " + ar.cause().getMessage());
            action.accept(false);
          }
          conn.close();
        }).onFailure(ar -> {
        action.accept(false);
        ar.printStackTrace();
      });
    });
  }

  public void updateActivityById(int id, JsonObject activity, Consumer<Boolean> action) {
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
      + "addr = ?"
      ;
    var sql = String.format("UPDATE `activity` SET %s WHERE `id` = ?", fields);
    conn().onSuccess(conn -> {
      conn
        .preparedQuery(sql)
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
        .onComplete(ar -> {
          if (ar.succeeded()) {
            RowSet<Row> rows = ar.result();
            var list = new JsonArray();
            for (Row row : rows) {
              var jo = row.toJson();
              list.add(jo);
            }
            action.accept(true);
          } else {
            System.out.println("Failure: " + ar.cause().getMessage());
            action.accept(false);
          }
          conn.close();
        }).onFailure(ar -> {
        action.accept(false);
        ar.printStackTrace();
      });
    });
  }
}
