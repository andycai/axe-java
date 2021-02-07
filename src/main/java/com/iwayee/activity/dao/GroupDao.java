package com.iwayee.activity.dao;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.function.Consumer;

public class GroupDao extends MySQLDao {
  public void create(JsonObject group, Consumer<Long> action) {
    var fields = "`level`,`name`,`members`,`activities`,`pending`,`notice`,`addr`,`logo`";
    var sql = String.format("INSERT INTO `group` (%s) VALUES (?,?,?,?,?,?,?,?)", fields);
    conn().onSuccess(conn -> {
      conn
        .preparedQuery(sql)
        .execute(Tuple.of(
          group.getInteger("level"),
          group.getString("name"),
          group.getJsonArray("members").encode(),
          group.getJsonArray("activities").encode(),
          group.getJsonArray("pending").encode(),
          group.getString("notice"),
          group.getString("addr"),
          group.getString("logo")
        ))
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
        ar.printStackTrace();
      });
    });
  }

  public void getGroupByID(int id, Consumer<JsonObject> action) {
    var fields = "`id`, `level`,`name`,`logo`,`members`, `pending`,`notice`,`addr`,`activities`";
    var sql = String.format("SELECT %s FROM `group` WHERE id=?", fields);
    conn().onSuccess(conn -> {
      conn
        .preparedQuery(sql)
        .execute(Tuple.of(id))
        .onComplete(ar -> {
          if (ar.succeeded()) {
            RowSet<Row> rows = ar.result();
            if (rows.size() > 0) {
              var data = new JsonObject();
              for (Row row : rows) {
                data = row.toJson();
              }
              action.accept(data);
            } else {
              System.out.println("Failure: " + ar.cause().getMessage());
              action.accept(null);
            }
          }
          conn.close();
        }).onFailure(ar -> {
          action.accept(null);
          ar.printStackTrace();
      });
    });
  }

  public void getGroups(int page, int num, Consumer<JsonArray> action) {
    var fields = "`id`, `level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    var sql = String.format("SELECT %s FROM `group` LIMIT %d,%d", fields, (page-1)*num, num);
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

  public void getGroupsByIds(String ids, Consumer<JsonArray> action) {
    var fields = "`id`, `level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    var sql = String.format("SELECT %s FROM `group` WHERE `id` IN(%s)", fields, ids);
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

  public void updateGroupById(int id, JsonObject group, Consumer<Boolean> action) {
    var fields = "level = ?, "
      + "name = ?, "
      + "logo = ?, "
      + "notice = ?, "
      + "addr = ?, "
      + "members = ?, "
      + "pending = ?, "
      + "activities = ?"
      ;
    var sql = String.format("UPDATE `group` SET %s WHERE `id` = ?", fields);
    conn().onSuccess(conn -> {
      conn
        .preparedQuery(sql)
        .execute(Tuple.of(
          group.getInteger("level"),
          group.getString("name"),
          group.getString("logo"),
          group.getString("notice"),
          group.getString("addr"),
          group.getJsonArray("members").encode(),
          group.getJsonArray("pending").encode(),
          group.getJsonArray("activities").encode(),
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
