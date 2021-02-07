package com.iwayee.activity.hub;

import com.google.common.base.Strings;
import com.iwayee.activity.define.RetCode;
import com.iwayee.activity.utils.TokenExpiredException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static com.google.common.base.Preconditions.*;

public class Some {
  private RoutingContext context;

  public Some(RoutingContext ctx) {
    this.context = ctx;
  }

  public int userId() {
    var token = getToken();
    return UserCache.getInstance().getCurrentUserId(token);
  }

  public void checkToken() {
    if (UserCache.getInstance().hasExpired(getToken())) {
      throw new TokenExpiredException("Login session was expired!");
    }
  }

  public String getToken() {
    var param = getJson();
    var token = param.getString("token");
    checkArgument(!Strings.isNullOrEmpty(token));
    return token;
  }

  public String getIP() {
    return context.request().remoteAddress().hostAddress();
  }

  public HttpServerRequest request() {
    return context.request();
  }

  public HttpServerResponse response() {
    return context.response();
  }

  public void json(Object json) {
    context.json(json);
  }

  public int getUint(String key) {
    var val = Integer.parseInt(context.request().getParam(key));
    checkArgument(val > 0);
    return val;
  }

  public int getInt(String key) {
    var val = Integer.parseInt(context.request().getParam(key));
    checkArgument(val >= 0);
    return val;
  }

  public String getStr(String key) {
    var val = context.request().getParam(key);
    checkArgument(!Strings.isNullOrEmpty(val));
    return val;
  }

  public JsonObject getJson() {
    var param = context.getBodyAsJson();
    checkArgument(param != null);
    return param;
  }

  public int jsonInt(String key) {
    var param = getJson();
    var val = param.getInteger(key);
    checkArgument(val >= 0);
    return val;
  }

  public int jsonUint(String key) {
    var param = getJson();
    var val = param.getInteger(key);
    checkArgument(val > 0);
    return val;
  }

  public String jsonStr(String key) {
    var param = getJson();
    var val = param.getString(key);
    checkArgument(!Strings.isNullOrEmpty(val));
    return val;
  }

  public boolean jsonBool(String key) {
    var param = getJson();
    return param.getBoolean(key);
  }

  // 响应前端
  public void ok(JsonArray data) {
    context.json(new JsonObject().put("code", 0).put("data", data));
  }

  public void ok(JsonObject data) {
    context.json(new JsonObject().put("code", 0).put("data", data));
  }

  public void succeed() {
    err(RetCode.SUCCESS);
  }

  public void msg(RetCode code) {
    err(code);
  }

  public void err(RetCode code) {
    err(code.getErrorCode(), code.getErrorDesc());
  }

  public void err(int code, String msg) {
    context.json(new JsonObject().put("code", code).put("msg", msg));
  }
}
