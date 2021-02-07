package com.iwayee.activity.config;

import io.vertx.core.json.JsonObject;

public class Redis {
  public int port;
  public int db;
  public String host;
  public String password;

  public void fromJson(JsonObject data) {
    port = data.getInteger("port");
    db = data.getInteger("db");
    host = data.getString("host");
    password = data.getString("password");
  }
}
