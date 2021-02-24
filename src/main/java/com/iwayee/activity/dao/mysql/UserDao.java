package com.iwayee.activity.dao.mysql;

import com.iwayee.activity.func.Action;
import com.iwayee.activity.func.Action2;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class UserDao extends MySQLDao {
  private static final Logger LOG = LoggerFactory.getLogger(UserDao.class);

  public void create(JsonObject user, Action2<Boolean, Long> action) {
    var fields =
            "username,password,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups";
    var sql = String.format("INSERT INTO user (%s) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", fields);

    db().preparedQuery(sql)
            .execute(
                    Tuple.of(
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
                            user.getString("groups")),
                    ar -> {
                      var lastInsertId = 0L;
                      if (ar.succeeded()) {
                        RowSet<Row> rows = ar.result();
                        lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID);
                        LOG.info("Last Insert Id: " + lastInsertId);
                      } else {
                        LOG.info("Failure: " + ar.cause().getMessage());
                      }
                      action.run(lastInsertId > 0, lastInsertId);
                    });
  }

  public void getUserByName(String username, Action2<Boolean, JsonObject> action) {
    var fields =
            "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    var sql = String.format("SELECT %s FROM `user` WHERE username = ?", fields);

    db().preparedQuery(sql).execute(Tuple.of(username), ar -> {
      JsonObject jo = null;
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        for (Row row : rows) {
          jo = toJo(row.toJson());
        }
      } else {
        LOG.info("Failure: " + ar.cause().getMessage());
      }
      action.run(Objects.nonNull(jo), jo);
    });
  }

  public void getUserById(long id, Action2<Boolean, JsonObject> action) {
    var fields =
            "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    var sql = String.format("SELECT %s FROM `user` WHERE id = ?", fields);

    db().preparedQuery(sql).execute(Tuple.of(id), ar -> {
      JsonObject jo = null;
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        for (Row row : rows) {
          jo = toJo(row.toJson());
        }
      } else {
        LOG.info("Failure: " + ar.cause().getMessage());
      }
      action.run(Objects.nonNull(jo), jo);
    });
  }

  public void getUsersByIds(String ids, Action2<Boolean, JsonObject> action) {
    var fields = "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups";
    var sql = String.format("SELECT %s FROM `user` WHERE id IN(%s)", fields, ids);

    db().preparedQuery(sql).execute(ar -> {
      var jo = new JsonObject();
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        for (Row row : rows) {
          jo.put(row.getInteger("id").toString(), toJo(row.toJson()));
        }
      } else {
        LOG.info("Failure: " + ar.cause().getMessage());
      }
      action.run(!jo.isEmpty(), jo);
    });
  }

  public void updateUserById(long id, JsonObject user, Action<Boolean> action) {
    var fields =
            "nick = ?, "
                    + "wx_nick = ?, "
                    + "token = ?, "
                    + "wx_token = ?, "
                    + "ip = ?, "
                    + "groups = ?, "
                    + "activities = ?";
    var sql = String.format("UPDATE `user` SET %s WHERE `id` = ?", fields);

    db().preparedQuery(sql).execute(Tuple.of(
            user.getString("nick"),
            user.getString("wx_nick"),
            user.getString("token"),
            user.getString("wx_token"),
            user.getString("ip"),
            user.getJsonArray("groups").encode(),
            user.getJsonArray("activities").encode(),
            id
    ), ar -> {
      var ret = false;
      if (ar.succeeded()) {
        ret = true;
      } else {
        LOG.info("Failure: " + ar.cause().getMessage());
      }
      action.run(ret);
    });
  }

  // 私有方法
  private JsonObject toJo(JsonObject jo) {
    jo.put("groups", new JsonArray(jo.getString("groups")));
    jo.put("activities", new JsonArray(jo.getString("activities")));
    return jo;
  }
}
