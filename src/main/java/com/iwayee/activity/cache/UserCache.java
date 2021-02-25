package com.iwayee.activity.cache;

import com.iwayee.activity.api.comp.Member;
import com.iwayee.activity.api.comp.Player;
import com.iwayee.activity.api.comp.Session;
import com.iwayee.activity.api.comp.User;
import com.iwayee.activity.utils.Singleton;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * TODO：缓存需要增加数量限制（LRU）
 */
public class UserCache extends BaseCache {
  private Map<String, User> usersForName = new HashMap<>();
  private Map<Long, User> usersForId = new HashMap<>();
  private Map<String, Session> sessions = new HashMap<>();
  private static final Logger LOG = LoggerFactory.getLogger(UserCache.class);

  public static UserCache getInstance() {
    return Singleton.instance(UserCache.class);
  }

  public void create(JsonObject jo, BiConsumer<Boolean, Long> action) {
    dao().user().create(jo, (b, data) -> {
      if (b) {
        jo.put("id", data);
        var user = jo.mapTo(User.class);
        cache(user);
      }
      action.accept(b, data);
    });
  }

  // 根据 username 获取用户数据
  public void getUserByName(String name, BiConsumer<Boolean, User> action) {
    if (usersForName.containsKey(name)) {
      action.accept(true, usersForName.get(name));
      LOG.info("从缓存中获取用户数据：" + name);
    } else {
      LOG.info("从DB中获取用户数据：" + name);
      dao().user().getUserByName(name, (b, data) -> {
        User user = null;
        if (b) {
          user = data.mapTo(User.class);
          cache(user);
        }
        action.accept(b, user);
      });
    }
  }

  // 根据 id 获取用户数据
  public void getUserById(long id, BiConsumer<Boolean, User> action) {
    if (usersForId.containsKey(id)) {
      LOG.info("从缓存中获取用户数据：" + id);
      action.accept(true, usersForId.get(id));
    } else {
      LOG.info("从DB中获取用户数据：" + id);
      dao().user().getUserById(id, (b, data) -> {
        User user = null;
        if (b) {
          user = data.mapTo(User.class);
          cache(user);
        }
        action.accept(b, user);
      });
    }
  }

  public JsonObject users2Json(Map<Long, User> usersMap) {
    var jo = new JsonObject();
    usersMap.forEach((key, value) -> {
      jo.put(key + "", JsonObject.mapFrom(value));
    });
    return jo;
  }

  public JsonObject toPlayer(Map<Long, User> usersMap) {
    var jo = new JsonObject();
    usersMap.forEach((key, value) -> {
      var player = new Player();
      player.fromUser(value);
      jo.put(key + "", player.toJson());
    });
    return jo;
  }

  public JsonArray toMember(Map<Long, User> usersMap, JsonArray members) {
    var jr = new JsonArray();
    for (var m : members) {
      var mb = ((JsonObject) m).mapTo(Member.class);
      mb.fromUser(usersMap.get(mb.id));
      jr.add(JsonObject.mapFrom(mb));
    }
    return jr;
  }

  // 批量获取用户数据
  public void getUsersByIds(List<Long> ids, BiConsumer<Boolean, Map<Long, User>> action) {
    if (ids.size() <= 0) {
      action.accept(false, null);
      return;
    }
    var idsFromDB = new ArrayList<Long>(); // 需要从DB获取数据的列表
    var usersMap = new HashMap<Long, User>();

    for (var id : ids) {
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
      LOG.info("从DB中获取用户数据：" + idStr);
      dao().user().getUsersByIds(idStr, (b, data) -> {
        if (b) {
          data.forEach(value -> {
            var jo = (JsonObject) value.getValue();
            var user = jo.mapTo(User.class);
            cache(user);
            usersMap.put(user.id, user);
          });
        }
        action.accept(b, usersMap);
      });
    } else {
      LOG.info("从缓存中获取用户数据：" + new JsonArray(ids).toString());
      action.accept(true, usersMap);
    }
  }

  public void cacheSession(String token, long uid, int sex) {
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
  public long currentId(String token) {
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

  public void syncToDB(long id, Consumer<Boolean> action) {
    if (usersForId.containsKey(id)) {
      var user = usersForId.get(id);
      dao().user().updateUserById(id, JsonObject.mapFrom(user), b -> {
        action.accept(b);
      });
      return;
    }
    action.accept(false);
  }

  // 私有方法
  private void cache(User user) {
    if (user != null) {
      usersForId.put(user.id, user);
      usersForName.put(user.username, user);
    }
  }
}
