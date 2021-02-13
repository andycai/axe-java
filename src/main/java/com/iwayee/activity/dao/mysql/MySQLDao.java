package com.iwayee.activity.dao.mysql;

import io.vertx.core.Future;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.SqlConnection;

public class MySQLDao {
  protected MySQLPool db() {
    return DaoMgr.getInstance().mysql();
  }

  protected Future<SqlConnection> conn() {
    return db().getConnection();
  }
}
