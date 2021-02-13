package com.iwayee.activity.hub;

import com.iwayee.activity.utils.Singleton;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class Hub {
  public Vertx vertx;
  private WebClient webClient;

  public static Hub getInstance() {
    return Singleton.instance(Hub.class);
  }

  public WebClient getWebClient() {
    if (webClient == null) {
      webClient = WebClient.create(Hub.getInstance().vertx);
    }
    return webClient;
  }
}
