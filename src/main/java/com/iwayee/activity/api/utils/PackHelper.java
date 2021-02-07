package com.iwayee.activity.api.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PackHelper {
  // [1,1,5,7,9,7,7,9]
  public static JsonArray packPlayers(String s) {
    var players = new JsonArray();
    var ids = new JsonArray(s);
    for (var id : ids) {
      //
    }

    return players;
  }
}
