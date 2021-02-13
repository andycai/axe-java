package com.iwayee.activity.utils;

import com.iwayee.activity.hub.Hub;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;

public class HttpUtils {
  public static void startServer(Vertx vertx, Router router) {
    var config = ConfigUtils.vo;
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(config.port)
      .onSuccess(server ->
        System.out.println(
          "HTTP server started on port " + server.actualPort()
        )
      );
  }

  public static void webClient() {
    // 创建WebClient，用于发送HTTP或者HTTPS请求
    WebClient webClient = WebClient.create(Hub.getInstance().vertx);
    // 以get方式请求远程地址
    webClient.getAbs("https://www.sina.com")
      .ssl(true)
      .send(handle -> {
        // 处理响应的结果
        if (handle.succeeded()) {
          // 这里拿到的结果就是一个HTML文本，直接打印出来
          System.out.println(handle.result().bodyAsString());
        }
      });
  }
}
