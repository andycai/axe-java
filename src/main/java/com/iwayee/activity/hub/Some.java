package com.iwayee.activity.hub;

import com.google.common.base.Strings;
import com.iwayee.activity.cache.UserCache;
import com.iwayee.activity.define.ErrCode;
import com.iwayee.activity.utils.TokenExpiredException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static com.google.common.base.Preconditions.*;

public class Some {
  private RoutingContext ctx;

  public Some(RoutingContext ctx) {
    this.ctx = ctx;
  }

  public int userId() {
    var token = getToken();
    return UserCache.getInstance().currentId(token);
  }

  public int userSex() {
    var token = getToken();
    return UserCache.getInstance().currentSex(token);
  }

  public void checkToken() {
    if (UserCache.getInstance().expired(getToken())) {
      throw new TokenExpiredException("Login session has expired!");
    }
  }

  public String getToken() {
    var param = getJson();
    var token = param.getString("token");
    checkArgument(!Strings.isNullOrEmpty(token));
    return token;
  }

  public String getIP() {
    return ctx.request().remoteAddress().hostAddress();
  }

  public HttpServerRequest request() {
    return ctx.request();
  }

  public HttpServerResponse response() {
    return ctx.response();
  }

  public void json(Object json) {
    ctx.json(json);
  }

  public int getUInt(String key) {
    var val = Integer.parseInt(ctx.request().getParam(key));
    checkArgument(val > 0);
    return val;
  }

  public int getInt(String key) {
    var val = Integer.parseInt(ctx.request().getParam(key));
    checkArgument(val >= 0);
    return val;
  }

  public String getStr(String key) {
    var val = ctx.request().getParam(key);
    checkArgument(!Strings.isNullOrEmpty(val));
    return val;
  }

  public JsonObject getJson() {
    var param = ctx.getBodyAsJson();
    checkArgument(param != null);
    return param;
  }

  public int jsonInt(String key) {
    var param = getJson();
    var val = param.getInteger(key);
    checkArgument(val >= 0);
    return val;
  }

  public int jsonUInt(String key) {
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
  public void ok(Object data) {
    var code = ErrCode.SUCCESS;
    ret(code.getErrorCode(), data);
  }

  public void succeed() {
    err(ErrCode.SUCCESS);
  }

  public void ret(int code, Object data) {
    ctx.json(new JsonObject().put("code", code).put("data", data));
  }

  public void err(ErrCode code) {
    msg(code.getErrorCode(), code.getErrorDesc());
  }

  public void msg(int code, String msg) {
    ctx.json(new JsonObject().put("code", code).put("msg", msg));
  }
}
