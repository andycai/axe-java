package com.iwayee.activity.dao.mysql;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GroupDao extends MySQLDao {
  private static final Logger LOG = LoggerFactory.getLogger(GroupDao.class);

  public void create(JsonObject group, BiConsumer<Boolean, Long> action) {
    var fields = "`level`,`name`,`members`,`activities`,`pending`,`notice`,`addr`,`logo`";
    var sql = String.format("INSERT INTO `group` (%s) VALUES (?,?,?,?,?,?,?,?)", fields);

    db().preparedQuery(sql)
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

  public void getGroupByID(int id, BiConsumer<Boolean, JsonObject> action) {
    var fields = "`id`,`scores`,`level`,`name`,`logo`,`members`, `pending`,`notice`,`addr`,`activities`";
    var sql = String.format("SELECT %s FROM `group` WHERE id=?", fields);

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

  public void getGroups(int page, int num, BiConsumer<Boolean, JsonArray> action) {
    var fields = "`id`,`scores`,`level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    var sql = String.format("SELECT %s FROM `group` ORDER BY id DESC LIMIT %d,%d", fields, (page - 1) * num, num);

    db().preparedQuery(sql)
            .execute()
            .onSuccess(rows -> {
              succeed(rows, action);
            })
            .onFailure(th -> {
              LOG.info("Failure: " + th.getMessage());
              action.accept(false, new JsonArray());
            });
  }

  public void getGroupsByIds(String ids, BiConsumer<Boolean, JsonArray> action) {
    var fields = "`id`,`scores`,`level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    var sql = String.format("SELECT %s FROM `group` WHERE `id` IN(%s)", fields, ids);

    db().preparedQuery(sql)
            .execute()
            .onSuccess(rows -> {
              succeed(rows, action);
            })
            .onFailure(th -> {
              LOG.info("Failure: " + th.getMessage());
              action.accept(false, new JsonArray());
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
            + "activities = ?";
    var sql = String.format("UPDATE `group` SET %s WHERE `id` = ?", fields);

    db().preparedQuery(sql)
            .execute(Tuple.of(
                    group.getInteger("level"),
                    group.getString("name"),
                    group.getString("logo"),
                    group.getString("notice"),
                    group.getString("addr"),
                    group.getJsonArray("members").encode(),
                    group.getJsonArray("pending").encode(),
                    group.getJsonArray("activities").encode(),
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
    jo.put("pending", new JsonArray(jo.getString("pending")));
    jo.put("activities", new JsonArray(jo.getString("activities")));
    return jo;
  }

  protected void succeed(RowSet<Row> rows, BiConsumer<Boolean, JsonArray> action) {
    var jr = new JsonArray();
    for (Row row : rows) {
      var jo = toJo(row.toJson());
      jr.add(jo);
    }
    action.accept(!jr.isEmpty(), jr);
  }
}
