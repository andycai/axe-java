package com.iwayee.activity.utils;

import com.iwayee.activity.config.Config;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Vertx;

public class ConfigUtils {
  public static Config vo;
//  private static ConfigUtils instance;

//  private ConfigUtils() {}
//
//  public synchronized static ConfigUtils getInstance() {
//    if (instance == null) {
//      instance = new ConfigUtils();
//    }
//    return instance;
//  }

  public static void getConfig(Vertx vertx, Runnable callback) {
    ConfigRetriever retriever = ConfigRetriever.create(vertx);
    retriever.getConfig(json -> {
      vo = new Config();
      vo.fromJson(json.result());
      System.out.println(String.format("config: %d %s %d", vo.port, vo.mysql.charset, vo.redis.port));
      callback.run();
    });
  }
}
