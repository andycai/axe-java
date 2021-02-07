package com.iwayee.activity.verticles;

import com.iwayee.activity.define.RetCode;
import com.iwayee.activity.hub.Go;
import com.iwayee.activity.api.system.*;
import com.iwayee.activity.hub.Some;
import com.iwayee.activity.utils.ConfigUtils;
import com.iwayee.activity.utils.HttpUtils;
import com.iwayee.activity.utils.Singleton;
import com.iwayee.activity.utils.TokenExpiredException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.function.Consumer;

public class MainVerticle extends AbstractVerticle {
  private Router router;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Go.getInstance().vertx = vertx;
    ConfigUtils.getConfig(vertx, () -> {
      startServer();
    });
  }

  private void errAuth(RoutingContext ctx) {
    var ret = RetCode.ERR_AUTH;
    ctx.json(new JsonObject().put("code", ret.getErrorCode()).put("msg", ret.getErrorDesc()));
  }

  private void errArg(RoutingContext ctx) {
    var ret = RetCode.ERR_PARAM;
    ctx.json(new JsonObject().put("code", ret.getErrorCode()).put("msg", ret.getErrorDesc()));
  }

  private void route(String s, BaseSystem sys, Consumer<Some> action) {
    route(s, sys, action, true);
  }

  private void runAction(RoutingContext ctx, BaseSystem sys, Consumer<Some> action, boolean auth) {
    try {
      var some = new Some(ctx);
      if (auth) {
        some.checkToken();
      }
      action.accept(some);
    } catch (IllegalArgumentException e) {
      errArg(ctx);
    } catch (TokenExpiredException e) {
      errAuth(ctx);
    }
  }

  private void route(String s, BaseSystem sys, Consumer<Some> action, boolean auth) {
    router.route(s).handler(ctx -> {
      runAction(ctx, sys, action, auth);
    });
  }

  private void get(String s, BaseSystem sys, Consumer<Some> action) {
    get(s, sys, action, true);
  }

  private void get(String s, BaseSystem sys, Consumer<Some> action, boolean auth) {
    router.get(s).handler(ctx -> {
      runAction(ctx, sys, action, auth);
    });
  }

  private void post(String s, BaseSystem sys, Consumer<Some> action) {
    post(s, sys, action, true);
  }

  private void post(String s, BaseSystem sys, Consumer<Some> action, boolean auth) {
    router.post(s).handler(ctx -> {
      runAction(ctx, sys, action, auth);
    });
  }

  private void put(String s, BaseSystem sys, Consumer<Some> action) {
    put(s, sys, action, true);
  }

  private void put(String s, BaseSystem sys, Consumer<Some> action, boolean auth) {
    router.put(s).handler(ctx -> {
      runAction(ctx, sys, action, auth);
    });
  }

  private void startServer() {
    router = Router.router(vertx);
    var test = Singleton.instance(TestSystem.class);
    var user = Singleton.instance(UserSystem.class);
    var group = Singleton.instance(GroupSystem.class);
    var act = Singleton.instance(ActivitySystem.class);

//    router.route().handler(CookieHandler.create());
//    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(BodyHandler.create());
    //to make session work correctly on Huawei & Mi phone, set Cookie Path to root '/'
//    Set<Cookie> ccList=routingContext.cookies();
//    for(Cookie ck:ccList){
//      ck.setPath("/");
//    }

    // 测试
    route("/test/:case", test, test::exec);
    route("/test", test, test::exec);

    // 用户
    get("/users/:uid", user, user::getUser);
    get("/users/your/groups", group, group::getGroupsByUserId);
    get("/users/your/activities", act, act::getActivitiesByUserId);

    post("/login", user, user::login, false);
    post("/login_wx", user, user::wxLogin, false);
    post("/register", user, user::register, false);
    post("/logout", user, user::logout);

    // 群组
    get("/groups/:gid", group, group::getGroupById);
    get("/groups", group, group::getGroups);
    get("/groups/:gid/pending", group, group::getApplyList);
    get("/groups/:gid/activities", act, act::getActivitiesByGroupId);

    post("/groups", group, group::createGroup);
    post("/groups/:gid/apply", group, group::apply);
    post("/groups/:gid/approve", group, group::approve);
    post("/groups/:gid/promote/:mid", group, group::promote);
    post("/groups/:gid/transfer/:mid", group, group::transfer);

    put("/groups/:gid", group, group::updateGroup);

    // 活动
    get("/activities/:aid", act, act::getActivityById);
    get("/activities", act, act::getActivities);

    post("/activities", act, act::createActivity);
    post("/activities/:aid/end", act, act::endActivity);
    post("/activities/:aid/apply", act, act::applyActivity);
    post("/activities/:aid/cancel", act, act::cancelActivity);
    post("/activities/:aid/bring", act, act::bringActivity);

    put("/activities/:aid", act, act::updateActivity);

    HttpUtils.startServer(vertx, router);
  }
}
