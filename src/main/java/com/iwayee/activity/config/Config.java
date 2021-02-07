package com.iwayee.activity.config;

import io.vertx.core.json.JsonObject;

public class Config {
  public int port;
  public MySQL mysql;
  public Redis redis;

  public void fromJson(JsonObject data) {
    port = data.getInteger("port");
    mysql = new MySQL();
    mysql.fromJson(data.getJsonObject("mysql"));
    redis = new Redis();
    redis.fromJson(data.getJsonObject("redis"));
  }
}
