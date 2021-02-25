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

public class UserDao extends MySQLDao {
  private static final Logger LOG = LoggerFactory.getLogger(UserDao.class);

  public void create(JsonObject user, BiConsumer<Boolean, Long> action) {
    String fields =
            "username,password,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups";
    String sql = String.format("INSERT INTO user (%s) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", fields);

    db().preparedQuery(sql)
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
                    user.getString("groups")))
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

  public void getUserByName(String username, BiConsumer<Boolean, JsonObject> action) {
    String fields =
            "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    String sql = String.format("SELECT %s FROM `user` WHERE username = ?", fields);

    db().preparedQuery(sql)
            .execute(Tuple.of(username))
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

  public void getUserById(long id, BiConsumer<Boolean, JsonObject> action) {
    String fields =
            "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    String sql = String.format("SELECT %s FROM `user` WHERE id = ?", fields);

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

  public void getUsersByIds(String ids, BiConsumer<Boolean, JsonObject> action) {
    String fields = "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups";
    String sql = String.format("SELECT %s FROM `user` WHERE id IN(%s)", fields, ids);

    db().preparedQuery(sql)
            .execute()
            .onSuccess(rows -> {
              JsonObject jo = new JsonObject();
              for (Row row : rows) {
                jo.put(row.getInteger("id").toString(), toJo(row.toJson()));
              }
              action.accept(!jo.isEmpty(), jo);
            })
            .onFailure(th -> {
              LOG.info("Failure: " + th.getMessage());
              action.accept(false, new JsonObject());
            });
  }

  public void updateUserById(long id, JsonObject user, Consumer<Boolean> action) {
    String fields =
            "nick = ?, "
                    + "wx_nick = ?, "
                    + "token = ?, "
                    + "wx_token = ?, "
                    + "ip = ?, "
                    + "groups = ?, "
                    + "activities = ?";
    String sql = String.format("UPDATE `user` SET %s WHERE `id` = ?", fields);

    db().preparedQuery(sql)
            .execute(Tuple.of(
                    user.getString("nick"),
                    user.getString("wx_nick"),
                    user.getString("token"),
                    user.getString("wx_token"),
                    user.getString("ip"),
                    user.getJsonArray("groups").encode(),
                    user.getJsonArray("activities").encode(),
                    id))
            .onSuccess(rows -> {
              action.accept(true);
            })
            .onFailure(th -> {
              LOG.info("Failure: " + th.getMessage());
              action.accept(false);
            });
  }

  // 私有方法
  private JsonObject toJo(JsonObject jo) {
    jo.put("groups", new JsonArray(jo.getString("groups")));
    jo.put("activities", new JsonArray(jo.getString("activities")));
    return jo;
  }
}
