package com.iwayee.activity.api.system;

import com.iwayee.activity.define.RetCode;
import com.iwayee.activity.hub.Some;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Date;

public class GroupSystem extends BaseSystem {
  public void getGroupById(Some some) {
    var gid = some.getUint("gid");

    cache().group().getGroupById(gid, data -> {
      if (data == null) {
        some.err(RetCode.ERR_DATA);
        return;
      }

      ArrayList<Integer> ids = new ArrayList<>();
      for (var m : data.members) {
        ids.add(((JsonObject)m).getInteger("id"));
      }
      if (ids.size() <= 0) {
        some.err(RetCode.ERR_DATA);
        return;
      }

      cache().user().getUsersByIds(ids, users -> {
        if (users == null) {
          some.err(RetCode.ERR_DATA);
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
    var page = some.jsonUint("page");
    var num = some.jsonUint("num");

    cache().group().getGroups(page, num, data -> {
      if (data == null) {
        some.err(RetCode.ERR_GROUP_GET_DATA);
        return;
      }
      some.ok(data);
    });
  }

  public void getGroupsByUserId(Some some) {
    cache().user().getUserById(some.userId(), user -> {
      var ids = user.groups.getList();
      cache().group().getGroupsByIds(ids, data -> {
        some.ok(data);
      });
    });
  }

  public void createGroup(Some some) {
    var jo = new JsonObject();
    jo.put("name", some.jsonStr("name"));
    jo.put("logo", some.jsonStr("logo"));
    jo.put("addr", some.jsonStr("addr"));

    cache().group().create(jo, some.userId(), groupId -> {
      if (groupId <= 0) {
        some.err(RetCode.ERR_OP);
        return;
      }
      some.ok(new JsonObject().put("group_id", groupId));
    });
  }

  public void updateGroup(Some some) {
    var gid = some.getUint("gid");
    var name = some.jsonStr("name");
    var addr = some.jsonStr("addr");
    var logo = some.jsonStr("logo");
    var notice = some.jsonStr("notice");
    cache().group().getGroupById(gid, group -> {
      if (group == null) {
        some.err(RetCode.ERR_GROUP_GET_DATA);
        return;
      }

      if (!group.isManager(some.userId())) {
        some.err(RetCode.ERR_GROUP_NOT_MANAGER);
        return;
      }

      group.name = name; // TODO:修改名字有次数限制
      group.addr = addr;
      group.logo = logo;
      group.notice = notice;
      dao().group().updateGroupById(gid, JsonObject.mapFrom(group), b -> {
        if (!b) {
          some.err(RetCode.ERR_GROUP_UPDATE_OP);
        }
        some.succeed();
      });
    });
  }

  public void getApplyList(Some some) {
    var gid = some.getUint("gid");
    cache().group().getGroupById(gid, group -> {
      if (group == null) {
        some.err(RetCode.ERR_GROUP_GET_DATA);
        return;
      }

      if (group.pending.size() > 0) {
        cache().user().getUsersByIds(group.pending.getList(), users -> {
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
    var gid = some.getUint("gid");
    var uid = some.userId();

    cache().group().getGroupById(gid, group -> {
      if (group == null) {
        some.err(RetCode.ERR_GROUP_GET_DATA);
        return;
      }

      if (!group.pending.contains(uid)) {
        group.pending.add(uid);
        // 持久化处理
        cache().group().syncToDB(group.id, b -> {
          if (!b) {
            some.err(RetCode.ERR_GROUP_UPDATE_OP);
            return;
          }
          some.succeed();
        });
      }
      some.succeed();
    });
  }

  // 审批申请
  public void approve(Some some) {
    var gid = some.getUint("gid");
    var pass = some.jsonBool("pass");
    var index = some.jsonInt("index");

    var uid = some.userId(); // 通过session获取
    cache().group().getGroupById(gid, group -> {
      if (group == null) {
        some.err(RetCode.ERR_GROUP_GET_DATA);
        return;
      }

      if (!group.isManager(uid) || index < 0 || index >= group.pending.size()) {
        some.err(RetCode.ERR_GROUP_APPROVE);
        return;
      }

      var tid = group.pending.getInteger(index);
      if (tid == null || tid < 0) {
        some.err(RetCode.ERR_GROUP_APPROVE);
        return;
      }

      if (!group.notIn(tid)) {
        some.err(RetCode.ERR_GROUP_APPROVE);
        return;
      }

      if (group.notIn(tid)) {
        if (pass) {
          var jo = new JsonObject();
          jo.put("id", tid);
          jo.put("pos", 1);
          jo.put("at", new Date().getTime());
          group.members.add(jo);
        }
        group.pending.remove(tid);
      }
      // 持久化处理
      cache().group().syncToDB(group.id, b -> {
        if (!b) {
          some.err(RetCode.ERR_GROUP_UPDATE_OP);
          return;
        }
        some.succeed();
      });
    });
  }

  // 提升管理员
  public void promote(Some some) {
    var gid = some.getUint("gid");
    var mid = some.getUint("mid");
    cache().group().getGroupById(gid, group -> {
      if (group == null) {
        some.err(RetCode.ERR_GROUP_GET_DATA);
        return;
      }

      if (!group.isOwner(some.userId())) {
        some.err(RetCode.ERR_GROUP_PROMOTE);
        return;
      }

      if (!group.promote(mid)) {
        some.err(RetCode.ERR_GROUP_PROMOTE);
        return;
      }

      dao().group().updateGroupById(gid, JsonObject.mapFrom(group), b -> {
        if (!b) {
          some.err(RetCode.ERR_GROUP_UPDATE_OP);
          return;
        }
        some.succeed();
      });
    });
  }

  // 转让群主
  public void transfer(Some some) {
    var gid = some.getUint("gid");
    var mid = some.getUint("mid");
    cache().group().getGroupById(gid, group -> {
      if (group == null) {
        some.err(RetCode.ERR_GROUP_GET_DATA);
        return;
      }

      if (!group.isOwner(some.userId())) {
        some.err(RetCode.ERR_GROUP_TRANSFER);
        return;
      }

      if (!group.transfer(some.userId(), mid)) {
        some.err(RetCode.ERR_GROUP_TRANSFER);
        return;
      }

      dao().group().updateGroupById(gid, JsonObject.mapFrom(group), b -> {
        if (!b) {
          some.err(RetCode.ERR_GROUP_UPDATE_OP);
          return;
        }
        some.succeed();
      });
    });
  }
}
