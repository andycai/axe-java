package com.iwayee.activity.api.system;

import com.iwayee.activity.api.comp.User;
import com.iwayee.activity.config.WeChat;
import com.iwayee.activity.define.ErrCode;
import com.iwayee.activity.hub.Hub;
import com.iwayee.activity.hub.Some;
import com.iwayee.activity.utils.EncryptUtil;
import com.iwayee.activity.utils.NetUtils;
import io.vertx.core.json.JsonObject;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class UserSystem extends BaseSystem {
  public void login(Some some) {
    var name = some.jsonStr("username");
    var wxNick = some.jsonStr("wx_nick");
    var sex = some.jsonUInt("sex");

    userCache().getUserByName(name, (b, user) -> {
      if (!b) {
        var ip = some.getIP();
        var jo = new JsonObject();
        jo.put("username", name)
                .put("password", EncryptUtil.getMD5Str("123456"))
                .put("token", EncryptUtil.getMD5Str(name))
                .put("wx_token", EncryptUtil.getMD5Str(name))
                .put("wx_nick", wxNick)
                .put("nick", "")
                .put("sex", sex)
                .put("phone", "")
                .put("email", "")
                .put("ip", ip)
                .put("activities", "[]")
                .put("groups", "[]")
        ;
        userCache().create(jo, (ok, uid) -> {
          if (ok) {
            var token = jo.getString("token");
            userCache().cacheSession(token, uid.intValue(), sex);
            userCache().getUserById(uid.intValue(), (ok2, user1) -> {
              if (ok2) {
                some.ok(user2Json(user1));
              } else {
                some.err(ErrCode.ERR_AUTH);
              }
            });
          } else {
            some.err(ErrCode.ERR_AUTH);
          }
        });
      } else {
        userCache().cacheSession(user.token, user.id, user.sex);
        some.ok(user2Json(user));
      }
    });
  }

  // 微信小程序登录
  // wx: js_code, get -> openid, session_key
  public void wxLogin(Some some) {
    var username = some.getStr("username");

    // 登录 session 过期，重新登了
    /*
    返回的 JSON 数据包
    属性	类型	说明
    openid	string	用户唯一标识
    session_key	string	会话密钥
    unionid	string	用户在开放平台的唯一标识符，在满足 UnionID 下发条件的情况下会返回，详见 UnionID 机制说明。
    errcode	number	错误码
    errmsg	string	错误信息
    */
    var url = String.format(WeChat.getSessionURL, "appid", "secret", "js_code");
    Hub.getInstance().getWebClient()
      .getAbs(url)
      .ssl(true)
      .send(ar -> {
        if (ar.succeeded()) {
          System.out.println(ar.result().bodyAsJsonObject().toString());

          // 查找数据库，用户是否已注册
          // 有数据就取出数据放进缓存，没有就先注册再取出数据放进缓存
        }
      });

//    dao().user().getUserByOpenid(param.getString("openid"), user -> {
//      if (user != null) {
//        succeed();
//      } else {
//        err(RetCode.ERR_LOGIN);
//      }
//    });
  }

  public void register(Some some) {
    // post
    try {
      InetAddress address = InetAddress.getLocalHost();
//      byte[] byteAddress = address.getAddress();
//      System.out.println(Arrays.toString(byteAddress));
//      System.out.println(address.getHostAddress());

      var param = some.getJson();
//      var ip = context.request().remoteAddress().hostAddress();
//      System.out.println(ip);
      param.put("ip", NetUtils.inet_aton(address.getHostAddress()));

      userCache().create(param, (b, newId) -> {
        if (b) {
          some.ok((new JsonObject()).put("user_id", newId));
        } else {
          some.err(ErrCode.ERR_REGISTER);
        }
      });
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }

  // 登出
  public void logout(Some some) {
    userCache().clearSession(some.getToken());
    some.succeed();
  }

  public void getUserByName(Some some) {
    var username = some.getStr("username");

    userCache().getUserByName(username, (b, user) -> {
      if (b) {
        some.ok(JsonObject.mapFrom(user));
      } else {
        some.err(ErrCode.ERR_DATA);
      }
    });
  }

  public void getUser(Some some) {
    var uid = some.getULong("uid");

    userCache().getUserById(uid, (ok, user) -> {
      if (ok) {
        some.ok(user2Json(user));
      } else some.err(ErrCode.ERR_USER_DATA);
    });
  }

  public void enterGroup(Some some, int gid, long uid) {
    userCache().getUserById(uid, (ok, user) -> {
      if (ok) {
        if (user.addGroup(gid)) saveData(some, uid);
        else some.succeed();
      } else some.err(ErrCode.ERR_USER_DATA);
    });
  }

  public void quitGroup(Some some, int gid, long uid) {
    userCache().getUserById(uid, (ok, user) -> {
      if (ok) {
        if (user.removeGroup(gid)) saveData(some, uid);
        else some.succeed();
      } else some.err(ErrCode.ERR_USER_DATA);
    });
  }

  public void applyActivity(Some some, long aid, long uid) {
    userCache().getUserById(uid, (ok, user) -> {
      if (ok) {
        if (user.addActivity(aid)) saveData(some, uid);
        else some.succeed();
      } else some.err(ErrCode.ERR_USER_DATA);
    });
  }

  public void cancelActivity(Some some, long aid, long uid) {
    userCache().getUserById(uid, (ok, user) -> {
      if (ok) {
        if (user.removeActivity(aid)) saveData(some, uid);
        else some.succeed();
      } else some.err(ErrCode.ERR_USER_DATA);
    });
  }

  // 私有方法
  private void saveData(Some some, long uid) {
    userCache().syncToDB(uid, ok -> {
      if (ok) some.succeed();
      else some.err(ErrCode.ERR_USER_UPDATE_DATA);
    });
  }

  private JsonObject user2Json(User user) {
    var jo = JsonObject.mapFrom(user);
    jo.remove("password");

    return jo;
  }
}
