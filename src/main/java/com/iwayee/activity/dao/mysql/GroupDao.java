package com.iwayee.activity.dao.mysql;

import com.iwayee.activity.func.Action;
import com.iwayee.activity.func.Action2;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.Objects;

public class GroupDao extends MySQLDao {
  public void create(JsonObject group, Action2<Boolean, Long> action) {
    var fields = "`level`,`name`,`members`,`activities`,`pending`,`notice`,`addr`,`logo`";
    var sql = String.format("INSERT INTO `group` (%s) VALUES (?,?,?,?,?,?,?,?)", fields);

    db().preparedQuery(sql).execute(Tuple.of(
            group.getInteger("level"),
            group.getString("name"),
            group.getJsonArray("members").encode(),
            group.getJsonArray("activities").encode(),
            group.getJsonArray("pending").encode(),
            group.getString("notice"),
            group.getString("addr"),
            group.getString("logo")
    ), ar -> {
      var lastInsertId = 0L;
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID);
        System.out.println("Last Insert Id: " + lastInsertId);
      } else {
        System.out.println("Failure: " + ar.cause().getMessage());
      }
      action.run(lastInsertId > 0, lastInsertId);
    });
  }

  public void getGroupByID(int id, Action2<Boolean, JsonObject> action) {
    var fields = "`id`,`scores`,`level`,`name`,`logo`,`members`, `pending`,`notice`,`addr`,`activities`";
    var sql = String.format("SELECT %s FROM `group` WHERE id=?", fields);

    db().preparedQuery(sql).execute(Tuple.of(id), ar -> {
      JsonObject jo = null;
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        if (rows.size() > 0) {
          for (Row row : rows) {
            jo = toJo(row.toJson());
          }
        } else {
          System.out.println("Failure: " + ar.cause().getMessage());
        }
      }
      action.run(Objects.nonNull(jo), jo);
    });
  }

  public void getGroups(int page, int num, Action2<Boolean, JsonArray> action) {
    var fields = "`id`,`scores`,`level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    var sql = String.format("SELECT %s FROM `group` ORDER BY id DESC LIMIT %d,%d", fields, (page - 1) * num, num);

    db().preparedQuery(sql).execute(ar -> {
      var jr = new JsonArray();
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        for (Row row : rows) {
          var jo = toJo(row.toJson());
          jr.add(jo);
        }
      } else {
        System.out.println("Failure: " + ar.cause().getMessage());
      }
      action.run(!jr.isEmpty(), jr);
    });
  }

  private JsonObject toJo(JsonObject jo) {
    jo.put("pending", new JsonArray(jo.getString("pending")));
    jo.put("activities", new JsonArray(jo.getString("activities")));
    return jo;
  }

  public void getGroupsByIds(String ids, Action2<Boolean, JsonArray> action) {
    var fields = "`id`,`scores`,`level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    var sql = String.format("SELECT %s FROM `group` WHERE `id` IN(%s)", fields, ids);

    db().preparedQuery(sql).execute(ar -> {
      var jr = new JsonArray();
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        for (Row row : rows) {
          var jo = toJo(row.toJson());
          jr.add(jo);
        }
      } else {
        System.out.println("Failure: " + ar.cause().getMessage());
      }
      action.run(!jr.isEmpty(), jr);
    });
  }

  public void updateGroupById(int id, JsonObject group, Action<Boolean> action) {
    var fields = "level = ?, "
            + "name = ?, "
            + "logo = ?, "
            + "notice = ?, "
            + "addr = ?, "
            + "members = ?, "
            + "pending = ?, "
            + "activities = ?";
    var sql = String.format("UPDATE `group` SET %s WHERE `id` = ?", fields);

    db().preparedQuery(sql).execute(Tuple.of(
            group.getInteger("level"),
            group.getString("name"),
            group.getString("logo"),
            group.getString("notice"),
            group.getString("addr"),
            group.getJsonArray("members").encode(),
            group.getJsonArray("pending").encode(),
            group.getJsonArray("activities").encode(),
            id
    ), ar -> {
      var ret = false;
      if (ar.succeeded()) {
        ret = true;
      } else {
        System.out.println("Failure: " + ar.cause().getMessage());
      }
      action.run(ret);
    });
  }
}
