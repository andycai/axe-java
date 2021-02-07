package com.iwayee.activity.hub;

import com.iwayee.activity.utils.Singleton;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class Go {
  public Vertx vertx;
  private WebClient webClient;

  public static Go getInstance() {
    return Singleton.instance(Go.class);
  }

  public WebClient getWebClient() {
    if (webClient == null) {
      webClient = WebClient.create(Go.getInstance().vertx);
    }
    return webClient;
  }
}
