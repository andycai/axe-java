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

    groupCache().getGroupById(gid, (ok, data) -> {
      if (!ok) {
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

      userCache().getUsersByIds(ids, (ok2, users) -> {
        if (!ok2) {
          some.err(ErrCode.ERR_DATA);
          return;
        }
        var members = userCache().toMember(users, data.members);
        var jo = JsonObject.mapFrom(data);
        jo.put("members", members);
        some.ok(jo);
      });
    });
  }

  public void getGroups(Some some) {
    var page = some.jsonUInt("page");
    var num = some.jsonUInt("num");

    groupCache().getGroups(page, num, (b, data) -> {
      some.ok(data);
    });
  }

  public void getGroupsByUserId(Some some) {
    userCache().getUserById(some.userId(), (ok, user) -> {
      if (ok) {
        var ids = user.groups;
        groupCache().getGroupsByIds(ids, (ok2, data) -> {
          if (ok2) {
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

    groupCache().create(jo, some.userId(), (ok, groupId) -> {
      if (ok) {
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
    groupCache().getGroupById(gid, (ok, group) -> {
      if (!ok) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
      } else if (!group.isManager(some.userId())) {
        some.err(ErrCode.ERR_GROUP_NON_MANAGER);
      } else {
        group.name = name; // TODO:修改名字有次数限制
        group.addr = addr;
        group.logo = logo;
        group.notice = notice;
        dao().group().updateGroupById(gid, JsonObject.mapFrom(group), ok2 -> {
          if (ok2) {
            some.succeed();
          } else {
            some.err(ErrCode.ERR_GROUP_UPDATE_OP);
          }
        });
      }
    });
  }

  public void getApplyList(Some some) {
    final var gid = some.getUInt("gid");
    groupCache().getGroupById(gid, (ok, group) -> {
      if (!ok) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
      } else if (group.pending.size() <= 0) {
        some.ok(new JsonArray());
      } else {
        userCache().getUsersByIds(group.pending, (ok2, users) -> {
          var jr = new JsonArray();
          users.forEach((key, val) -> {
            var jo = new JsonObject();
            jo.put("id", val.id);
            jo.put("nick", val.nick);
            jo.put("wx_nick", val.wx_nick);
            var index = group.pending.indexOf(val.id);
            jo.put("index", index);
            jr.add(jo);
          });
          some.ok(jr);
        });
      }
    });
  }

  public void apply(Some some) {
    final var gid = some.getUInt("gid");
    final var uid = some.userId();

    groupCache().getGroupById(gid, (ok, group) -> {
      if (!ok) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
      } else if (group.pending.contains(uid)) {
        some.succeed();
      } else {
        group.pending.add(uid);
        saveData(some, gid);
      }
    });
  }

  // 审批申请
  public void approve(Some some) {
    final var gid = some.getUInt("gid");
    final var pass = some.jsonBool("pass");
    final var index = some.jsonInt("index");
    final var uid = some.userId(); // 通过session获取
    groupCache().getGroupById(gid, (ok, group) -> {
      if (!ok) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
      } else if (!group.isManager(uid)) {
        some.err(ErrCode.ERR_GROUP_NON_MANAGER);
      } else if (group.notInPending(index)) {
        some.err(ErrCode.ERR_GROUP_APPROVE);
      } else {
        var tid = group.pending.get(index);
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
          saveData(some, gid);
        } else {
          some.err(ErrCode.ERR_GROUP_APPROVE);
        }
      }
    });
  }

  // 提升管理员
  public void promote(Some some) {
    final var gid = some.getUInt("gid");
    final var mid = some.getULong("mid");
    final var uid = some.userId();
    groupCache().getGroupById(gid, (ok, group) -> {
      if (!ok) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
      } else if (!group.isOwner(uid)) {
        some.err(ErrCode.ERR_GROUP_NON_OWNER);
      } else if (group.managerCount() >= 3) { // 不能超过3个副群主
        some.err(ErrCode.ERR_GROUP_MANAGER_LIMIT);
      } else if (!group.promote(mid)) {
        some.err(ErrCode.ERR_GROUP_PROMOTE);
      } else {
        saveData(some, gid);
      }
    });
  }

  // 转让群主
  public void transfer(Some some) {
    final var gid = some.getUInt("gid");
    final var mid = some.getULong("mid");
    final var uid = some.userId();
    groupCache().getGroupById(gid, (ok, group) -> {
      if (!ok) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
      } else if (!group.isOwner(uid)) {
        some.err(ErrCode.ERR_GROUP_NON_OWNER);
      } else if (!group.transfer(uid, mid)) {
        some.err(ErrCode.ERR_GROUP_TRANSFER);
      } else {
        saveData(some, gid);
      }
    });
  }

  // 移除群成员
  public void remove(Some some) {
    final var gid = some.getUInt("gid");
    final var mid = some.getULong("mid");
    final var uid = some.userId();
    groupCache().getGroupById(gid, (ok, group) -> {
      if (!ok) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
      } else if (!group.isManager(uid)) {
        some.err(ErrCode.ERR_GROUP_NON_MANAGER);
      } else if (!group.remove(mid)) {
        some.err(ErrCode.ERR_GROUP_REMOVE);
      } else {
        saveData(some, gid);
      }
    });
  }

  // 退出群组
  public void quit(Some some) {
    final var gid = some.getUInt("gid");
    final var uid = some.userId();
    groupCache().getGroupById(gid, (ok, group) -> {
      if (!ok) {
        some.err(ErrCode.ERR_GROUP_GET_DATA);
      } else if (!group.isMember(uid)) {
        some.err(ErrCode.ERR_GROUP_NON_MEMBER);
      } else if (!group.remove(uid)) {
        some.err(ErrCode.ERR_GROUP_REMOVE);
      } else {
        saveData(some, gid);
      }
    });
  }

  // 私有方法
  public void saveData(Some some, int id) {
    groupCache().syncToDB(id, b -> {
      if (b) {
        some.succeed();
      } else {
        some.err(ErrCode.ERR_GROUP_UPDATE_OP);
      }
    });
  }
}
