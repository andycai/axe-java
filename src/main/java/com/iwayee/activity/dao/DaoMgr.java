package com.iwayee.activity.dao;

import com.iwayee.activity.hub.Go;
import com.iwayee.activity.utils.ConfigUtils;
import com.iwayee.activity.utils.Singleton;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;

public class DaoMgr {
  private MySQLPool mysql;

  public static DaoMgr getInstance() {
    return Singleton.instance(DaoMgr.class);
  }

  public static MySQLPool createClient() {
    var config = ConfigUtils.vo;
    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
      .setPort(config.mysql.port)
      .setHost(config.mysql.host)
      .setDatabase(config.mysql.db)
      .setUser(config.mysql.username)
      .setPassword(config.mysql.password)
      .setCharset(config.mysql.charset);

    // Pool options
    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(config.mysql.pool_max);

    // Create the client pool
    MySQLPool client = MySQLPool.pool(Go.getInstance().vertx, connectOptions, poolOptions);

    return client;
  }

  public MySQLPool mysql() {
    if (mysql == null) {
      mysql = createClient();
      System.out.println("创建 MySQL 客户端");
    }
    return mysql;
  }

  public void closeMysql() {
    if (mysql != null) {
      mysql.close();
    }
  }

  // 获取DAO
  public UserDao user() {
    return Singleton.instance(UserDao.class);
  }

  public GroupDao group() {
    return Singleton.instance(GroupDao.class);
  }

  public ActivityDao act() {
    return Singleton.instance(ActivityDao.class);
  }
}
