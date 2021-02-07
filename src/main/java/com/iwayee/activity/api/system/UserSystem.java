package com.iwayee.activity.api.system;

import com.google.common.base.Strings;
import com.iwayee.activity.api.comp.User;
import com.iwayee.activity.hub.Some;
import com.iwayee.activity.hub.UserCache;
import com.iwayee.activity.config.WeChat;
import com.iwayee.activity.define.RetCode;
import com.iwayee.activity.hub.Go;
import com.iwayee.activity.utils.EncryptUtil;
import com.iwayee.activity.utils.NetUtils;
import io.vertx.core.json.JsonObject;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.google.common.base.Preconditions.*;

public class UserSystem extends BaseSystem {
  private JsonObject user2Json(User user) {
    var jo = JsonObject.mapFrom(user);
    jo.remove("password");

    return jo;
  }

  public void login(Some some) {
    var username = some.jsonStr("username");
    var wxNick = some.jsonStr("wx_nick");
    var sex = some.jsonUint("sex");

    cache().user().getUserByName(username, user -> {
      if (user == null) {
        var ip = some.getIP();
        var jo = new JsonObject();
        jo.put("username", username)
          .put("password", "111111")
          .put("token", EncryptUtil.getMD5Str(username))
          .put("wx_token", EncryptUtil.getMD5Str(username))
          .put("wx_nick", wxNick)
          .put("nick", "")
          .put("sex", sex)
          .put("phone", "")
          .put("email", "")
          .put("ip", ip)
          .put("activities", "[]")
          .put("groups", "[]")
          ;
        cache().user().createUser(jo, uid -> {
          if (uid > 0) {
            var token = jo.getString("token");
            cache().user().cacheSession(token, uid.intValue(), sex);
            cache().user().getUserById(uid.intValue(), user1 -> {
              some.ok(user2Json(user1));
            });
          } else {
            some.err(RetCode.ERR_AUTH);
          }
        });
      } else {
        cache().user().cacheSession(user.token, user.id, user.sex);
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
    Go.getInstance().getWebClient()
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

      cache().user().createUser(param, lastInsertId -> {
        if (lastInsertId > 0) {
          some.ok((new JsonObject()).put("user_id", lastInsertId));
        } else {
          some.err(RetCode.ERR_REGISTER);
        }
      });
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }

  // 登出
  public void logout(Some some) {
    cache().user().clearSession(some.getToken());
  }

  public void getUserByUsername(Some some) {
    var username = some.getStr("username");
    checkArgument(!Strings.isNullOrEmpty(username));

    cache().user().getUserByName(username, user -> {
      if (user == null) {
        some.err(RetCode.ERR_DATA);
      } else {
        some.ok(JsonObject.mapFrom(user));
      }
    });
  }

  public void getUser(Some some) {
    var uid = some.getInt("uid");
    checkArgument(uid > 0);

    cache().user().getUserById(uid, user -> {
      if (user == null) {
        some.err(RetCode.ERR_DATA);
      } else {
        some.ok(user2Json(user));
      }
    });
  }
}
