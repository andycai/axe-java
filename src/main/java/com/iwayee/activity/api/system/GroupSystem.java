package com.iwayee.activity.api.system;

import com.iwayee.activity.define.ErrCode;
import com.iwayee.activity.hub.Some;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Date;

public class GroupSystem extends BaseSystem {
  public void getGroupById(Some some) {
    var gid = some.getUInt("gid");

    cache().group().getGroupById(gid, (b, data) -> {
      if (!b) {
        some.err(ErrCode.ERR_DATA);
        return;
      }

      ArrayList<Long> ids = new ArrayList<>();
      for (var m : data.members) {
        ids.add(((JsonObject) m).getLong("id"));
      }
      if (ids.size() <= 0) {
        some.err(ErrCode.ERR_DATA);
        return;
      }

      cache().user().getUsersByIds(ids, (isOK, users) -> {
        if (!isOK) {
          some.err(ErrCode.ERR_DATA);
          return;
        }
        var members = cache().user().toMember(users, data.members);
        var jo = JsonObject.mapFrom(data);
        jo.put("members", members);
        some.ok(jo);
      });
    });
  }

  public void getGroups(Some some) {
    var page = some.jsonUInt("page");
    var num = some.jsonUInt("num");

    cache().group().getGroups(page, num, (b, data) -> {
      some.ok(data);
    });
  }

  public void getGroupsByUserId(Some some) {
    cache().user().getUserById(some.userId(), (b, user) -> {
      if (b) {
        var ids = user.groups.getList();
        cache().group().getGroupsByIds(ids, (isOK, data) -> {
          if (isOK) {
            some.ok(data);
          } else {
            some.err(ErrCode.ERR_GROUP_GET_DATA);
          }
        });
      } else {
        some.err(ErrCode.ERR_USER_DATA);
      }
    });
  }

  public void create(Some some) {
    var jo = new JsonObject();
    jo.put("name", some.jsonStr("name"));
    jo.put("logo", some.jsonStr("logo"));
    jo.put("addr", some.jsonStr("addr"));

    cache().group().create(jo, some.userId(), (b, groupId) -> {
      if (b) {
        some.ok(new JsonObject().put("group_id", groupId));
      } else {
        some.err(ErrCode.ERR_OP);
      }
    });
  }

  public void updateGroup(Some some) {
    var gid = some.getUInt("gid");
    var name = some.jsonStr("name");
    var addr = some.jsonStr("addr");
    var logo = some.jsonStr("logo");
    var notice = some.jsonStr("notice");
    cache().group().getGroupById(gid, (b, group) -> {
      if (!b) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
      } else if (!group.isManager(some.userId())) {
        some.err(ErrCode.ERR_GROUP_NOT_MANAGER);
      } else {
        group.name = name; // TODO:修改名字有次数限制
        group.addr = addr;
        group.logo = logo;
        group.notice = notice;
        dao().group().updateGroupById(gid, JsonObject.mapFrom(group), isOK -> {
          if (isOK) {
            some.succeed();
          } else {
            some.err(ErrCode.ERR_GROUP_UPDATE_OP);
          }
        });
      }
    });
  }

  public void getApplyList(Some some) {
    var gid = some.getUInt("gid");
    cache().group().getGroupById(gid, (b, group) -> {
      if (!b) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
        return;
      }

      if (group.pending.size() > 0) {
        cache().user().getUsersByIds(group.pending.getList(), (isOK, users) -> {
          var jr = new JsonArray();
          users.forEach((key, val) -> {
            var jo = new JsonObject();
            jo.put("id", val.id);
            jo.put("nick", val.nick);
            jo.put("wx_nick", val.wx_nick);
            var index = group.pending.getList().indexOf(val.id);
            jo.put("index", index);
            jr.add(jo);
          });
          some.ok(jr);
        });
        return;
      }
      some.ok(new JsonArray());
    });
  }

  public void apply(Some some) {
    var gid = some.getUInt("gid");
    var uid = some.userId();

    cache().group().getGroupById(gid, (isOK, group) -> {
      if (isOK && !group.pending.contains(uid)) {
        group.pending.add(uid);
        // 持久化处理
        cache().group().syncToDB(group.id, b -> {
          if (b) {
            some.succeed();
          } else {
            some.err(ErrCode.ERR_GROUP_UPDATE_OP);
          }
        });
      } else {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
      }
    });
  }

  // 审批申请
  public void approve(Some some) {
    var gid = some.getUInt("gid");
    var pass = some.jsonBool("pass");
    var index = some.jsonInt("index");

    var uid = some.userId(); // 通过session获取
    cache().group().getGroupById(gid, (isOK, group) -> {
      if (!isOK) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
        return;
      }

      if (!group.isManager(uid) || index >= group.pending.size()) {
        some.err(ErrCode.ERR_GROUP_APPROVE);
        return;
      }

      var tid = group.pending.getInteger(index);
      if (group.notIn(tid)) {
        if (pass) {
          var jo = new JsonObject();
          jo.put("id", tid);
          jo.put("scores", 0);
          jo.put("pos", 1);
          jo.put("at", new Date().getTime());
          group.members.add(jo);
        }
        group.pending.remove(tid);

        // 持久化处理
        cache().group().syncToDB(group.id, b -> {
          if (b) {
            some.succeed();
          } else {
            some.err(ErrCode.ERR_GROUP_UPDATE_OP);
          }
        });
      } else {
        some.err(ErrCode.ERR_GROUP_APPROVE);
      }
    });
  }

  // 提升管理员
  public void promote(Some some) {
    var gid = some.getUInt("gid");
    var mid = some.getULong("mid");
    var uid = some.userId();
    cache().group().getGroupById(gid, (isOK, group) -> {
      if (!isOK) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
        return;
      }

      if (!group.isOwner(uid)) {
        some.err(ErrCode.ERR_GROUP_PROMOTE);
        return;
      }

      // 不能超过3个副群主
      if (group.managerCount() >= 3) {
        some.err(ErrCode.ERR_GROUP_MANAGER_LIMIT);
        return;
      }

      if (!group.promote(mid)) {
        some.err(ErrCode.ERR_GROUP_PROMOTE);
        return;
      }

      cache().group().syncToDB(group.id, b -> {
        if (b) {
          some.succeed();
        } else {
          some.err(ErrCode.ERR_GROUP_UPDATE_OP);
        }
      });
    });
  }

  // 转让群主
  public void transfer(Some some) {
    var gid = some.getUInt("gid");
    var mid = some.getULong("mid");
    var uid = some.userId();
    cache().group().getGroupById(gid, (isOK, group) -> {
      if (!isOK) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
        return;
      }

      if (!group.isOwner(uid)) {
        some.err(ErrCode.ERR_GROUP_TRANSFER);
        return;
      }

      if (!group.transfer(uid, mid)) {
        some.err(ErrCode.ERR_GROUP_TRANSFER);
        return;
      }

      cache().group().syncToDB(group.id, b -> {
        if (b) {
          some.succeed();
        } else {
          some.err(ErrCode.ERR_GROUP_UPDATE_OP);
        }
      });
    });
  }
}
