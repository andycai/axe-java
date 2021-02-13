package com.iwayee.activity.config;

import io.vertx.core.json.JsonObject;

public class Config {
  public int port;
  public MySQL mysql;
  public Redis redis;

  public void fromJson(JsonObject data) {
    port = data.getInteger("port");
    if (mysql == null) {
      mysql = data.getJsonObject("mysql").mapTo(MySQL.class);
    }
    if (redis == null) {
      redis = data.getJsonObject("redis").mapTo(Redis.class);
    }
  }
}
