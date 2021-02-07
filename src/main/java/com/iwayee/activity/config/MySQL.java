package com.iwayee.activity.config;

import io.vertx.core.json.JsonObject;

public class MySQL {
  public int port;
  public int pool_max;
  public String host;
  public String db;
  public String username;
  public String password;
  public String charset;

  public void fromJson(JsonObject data) {
    port = data.getInteger("port");
    pool_max = data.getInteger("pool_max");
    host = data.getString("host");
    db = data.getString("db");
    username = data.getString("username");
    password = data.getString("password");
    charset = data.getString("charset");
  }
}
