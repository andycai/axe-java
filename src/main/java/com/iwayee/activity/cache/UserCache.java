package com.iwayee.activity.cache;

import com.iwayee.activity.api.comp.Member;
import com.iwayee.activity.api.comp.Player;
import com.iwayee.activity.api.comp.Session;
import com.iwayee.activity.api.comp.User;
import com.iwayee.activity.utils.Singleton;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Consumer;

public class UserCache extends BaseCache {
  private Map<String, User> usersForName = new HashMap<>();
  private Map<Integer, User> usersForId = new HashMap<>();
  private Map<String, Session> sessions = new HashMap<>();

  public static UserCache getInstance() {
    return Singleton.instance(UserCache.class);
  }

  private void cache(User user) {
    if (user != null) {
      usersForId.put(user.id, user);
      usersForName.put(user.username, user);
    }
  }

  public void create(JsonObject jo, Consumer<Long> action) {
    dao().user().create(jo, data -> {
      if (data > 0) {
        jo.put("id", data.intValue());
        var user = jo.mapTo(User.class);
        cache(user);
      }
      action.accept(data);
    });
  }

  // 根据 username 获取用户数据
  public void getUserByName(String name, Consumer<User> action) {
    if (usersForName.containsKey(name)) {
      action.accept(usersForName.get(name));
      System.out.println("从缓存中获取用户数据：" + name);
    } else {
      System.out.println("从DB中获取用户数据：" + name);
      dao().user().getUserByName(name, data -> {
        User user = null;
        if (data != null) {
          user = data.mapTo(User.class);
          cache(user);
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
      dao().user().getUserById(id, data -> {
        User user = null;
        if (data != null) {
          user = data.mapTo(User.class);
          cache(user);
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
      dao().user().getUsersByIds(idStr, data -> {
        if (data != null) {
          data.forEach(value -> {
            var jo = (JsonObject) value.getValue();
            var user = jo.mapTo(User.class);
            cache(user);
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

  public void cacheSession(String token, int uid, int sex) {
    Session session = null;
    if (sessions.containsKey(token)) {
      session = sessions.get(token);
    } else {
      session = new Session();
      sessions.put(token, session);
    }
    session.token = token;
    session.uid = uid;
    session.sex = sex;
    session.at = new Date().getTime();
  }

  public void clearSession(String token) {
    if (sessions.containsKey(token)) {
      sessions.remove(token);
    }
  }

  // 当前用户id
  public int currentId(String token) {
    var session = sessions.get(token);
    if (session != null) {
      return session.uid;
    }
    return 0;
  }

  public int currentSex(String token) {
    var session = sessions.get(token);
    if (session != null) {
      return session.sex;
    }
    return 1;
  }

  public boolean expired(String token) {
    var session = sessions.get(token);
    if (session != null) {
      var now = new Date().getTime();
      return (now - session.at) > (2 * 24 * 60 * 60 * 1000);
    }
    return true;
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
