package com.iwayee.activity.hub;

import com.iwayee.activity.api.comp.Member;
import com.iwayee.activity.api.comp.Player;
import com.iwayee.activity.api.comp.User;
import com.iwayee.activity.api.comp.UserSession;
import com.iwayee.activity.utils.Singleton;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Consumer;

public class UserCache extends BaseCache {
  public Map<String, User> usersForName = new HashMap<>();
  public Map<Integer, User> usersForId = new HashMap<>();
  private Map<String, UserSession> sessions = new HashMap<>();

  public static UserCache getInstance() {
    return Singleton.instance(UserCache.class);
  }

  private void cacheUser(User user) {
    if (user != null) {
      usersForId.put(user.id, user);
      usersForName.put(user.username, user);
    }
  }

  public void createUser(JsonObject jo, Consumer<Long> action) {
    dao().user().create(jo, data -> {
      if (data > 0) {
        jo.put("id", data.intValue());
        var user = jo.mapTo(User.class);
        cacheUser(user);
      }
      action.accept(data);
    });
  }

  // 根据 username 获取用户数据
  public void getUserByName(String username, Consumer<User> action) {
    if (usersForName.containsKey(username)) {
      action.accept(usersForName.get(username));
      System.out.println("从缓存中获取用户数据：" + username);
    } else {
      System.out.println("从DB中获取用户数据：" + username);
      dao().user().getUserByUsername(username, data -> {
        User user = null;
        if (data != null) {
          user = data.mapTo(User.class);
          cacheUser(user);
        }
        action.accept(user);
      });
    }
  }

  // 根据 id 获取用户数据
  public void getUserById(Integer id, Consumer<User> action) {
    if (usersForId.containsKey(id)) {
      System.out.println("从缓存中获取用户数据：" + id);
      action.accept(usersForId.get(id));
    } else {
      System.out.println("从DB中获取用户数据：" + id);
      dao().user().getUserByID(id, data -> {
        User user = null;
        if (data != null) {
          user = data.mapTo(User.class);
          cacheUser(user);
        }
        action.accept(user);
      });
    }
  }

  public JsonObject users2Json(Map<Integer, User> usersMap) {
    var jo = new JsonObject();
    usersMap.forEach((key, value) -> {
      jo.put(key+"", JsonObject.mapFrom(value));
    });
    return jo;
  }

  public JsonObject toPlayer(Map<Integer, User> usersMap) {
    var jo = new JsonObject();
    usersMap.forEach((key, value) -> {
      var player = new Player();
      player.fromUser(value);
      jo.put(key+"", player.toJson());
    });
    return jo;
  }

  public JsonArray toMember(Map<Integer, User> usersMap, JsonArray members) {
    var jr = new JsonArray();
    for (var m : members) {
      var mb = ((JsonObject)m).mapTo(Member.class);
      mb.fromUser(usersMap.get(mb.id));
      jr.add(JsonObject.mapFrom(mb));
    }
    return jr;
  }

  // 批量获取用户数据
  public void getUsersByIds(List<Integer> ids, Consumer<Map<Integer, User>> action) {
    if (ids.size() <= 0) {
      action.accept(null);
      return;
    }
    var idsFromDB = new ArrayList<Integer>(); // 需要从DB获取数据的列表
    var usersMap = new HashMap<Integer, User>();
    Iterator<Integer> it = ids.iterator();
    while (it.hasNext()) {
      Integer id = it.next();
      if (!usersMap.containsKey(id)) {
        if (usersForId.containsKey(id)) {
          usersMap.put(id, usersForId.get(id));
        } else {
          idsFromDB.add(id);
        }
      }
    }

    // 需要从DB获取
    if (idsFromDB.size() > 0) {
      String idStr = joiner.join(idsFromDB);
      System.out.println("从DB中获取用户数据：" + idStr);
      dao().user().getUsersByList(idStr, data -> {
        if (data != null) {
          data.forEach(value -> {
            var jo = (JsonObject) value.getValue();
            var user = jo.mapTo(User.class);
            cacheUser(user);
            usersMap.put(user.id, user);
          });
        }
        action.accept(usersMap);
      });
    } else {
      System.out.println("从缓存中获取用户数据：" + new JsonArray(ids).toString());
      action.accept(usersMap);
    }
  }

  public void cacheSession(String token, int uid) {
    var session = new UserSession();
    session.token = token;
    session.uid = uid;
    session.at = new Date().getTime();
    sessions.put(session.token, session);
  }

  public void clearSession(String token) {
    if (sessions.containsKey(token)) {
      sessions.remove(token);
    }
  }

  public int getCurrentUserId(String token) {
    var session = sessions.get(token);
    if (session == null) {
      return 0;
    } else {
      return session.uid;
    }
  }

  public boolean hasExpired(String token) {
    var session = sessions.get(token);
    if (session == null) {
      return true;
    }
    var now = new Date().getTime();
    if (now - session.at > (2*24*60*60*1000)) {
      return true;
    }
    return false;
  }

  public void syncToDB(int id, Consumer<Boolean> action) {
    if (usersForId.containsKey(id)) {
      var user = usersForId.get(id);
      dao().user().updateUserById(id, JsonObject.mapFrom(user), b -> {
        action.accept(b);
      });
      return;
    }
    action.accept(false);
  }
}
