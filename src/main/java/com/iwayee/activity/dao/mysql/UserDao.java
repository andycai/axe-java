package com.iwayee.activity.dao.mysql;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.function.Consumer;

public class UserDao extends MySQLDao {
  public void create(JsonObject user, Consumer<Long> action) {
    var fields = "username,password,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups";
    var sql = String.format("INSERT INTO user (%s) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", fields);
    conn().onSuccess(conn -> {
      conn
        .preparedQuery(sql)
        .execute(Tuple.of(
          user.getString("username"),
          user.getString("password"),
          user.getString("token"),
          user.getString("nick"),
          user.getString("wx_token"),
          user.getString("wx_nick"),
          user.getInteger("sex"),
          user.getString("phone"),
          user.getString("email"),
          user.getString("ip"),
          user.getString("activities"),
          user.getString("groups")
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

  public void getUserByUsername(String username, Consumer<JsonObject> action) {
    var fields = "id,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    var sql = String.format("SELECT %s FROM `user` WHERE username = ?", fields);
    conn().onSuccess(conn -> {
      conn
        .preparedQuery(sql)
        .execute(Tuple.of(username))
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

  public void getUserByID(int id, Consumer<JsonObject> action) {
    var fields = "id,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    var sql = String.format("SELECT %s FROM `user` WHERE id = ?", fields);
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

  public void getUsersByList(String ins, Consumer<JsonObject> action) {
    var fields = "id,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups";
    var sql = String.format("SELECT %s FROM `user` WHERE id IN(%s)", fields, ins);
    conn().onSuccess(conn -> {
      conn.preparedQuery(sql)
        .execute()
        .onComplete(ar -> {
          if (ar.succeeded()) {
            RowSet<Row> rows = ar.result();
            var jo = new JsonObject();
            for (Row row : rows) {
              jo.put(row.getInteger("id").toString(), row.toJson());
            }
            action.accept(jo);
          } else {
            System.out.println("Failure: " + ar.cause().getMessage());
            action.accept(new JsonObject());
          }
          conn.close();
        }).onFailure(ar -> {
          action.accept(new JsonObject());
          ar.printStackTrace();
      });
    });
  }

  public void updateUserById(int id, JsonObject user, Consumer<Boolean> action) {
    var fields = "nick = ?, "
      + "wx_nick = ?, "
      + "token = ?, "
      + "wx_token = ?, "
      + "ip = ?, "
      + "groups = ?, "
      + "activities = ?"
      ;
    var sql = String.format("UPDATE `user` SET %s WHERE `id` = ?", fields);
    conn().onSuccess(conn -> {
      conn
        .preparedQuery(sql)
        .execute(Tuple.of(
          user.getString("nick"),
          user.getString("wx_nick"),
          user.getString("token"),
          user.getString("wx_token"),
          user.getString("ip"),
          user.getJsonArray("groups").encode(),
          user.getJsonArray("activities").encode(),
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
