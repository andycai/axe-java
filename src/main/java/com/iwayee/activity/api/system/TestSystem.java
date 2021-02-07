package com.iwayee.activity.api.system;

import com.iwayee.activity.api.comp.Member;
import com.iwayee.activity.dao.UserDao;
import com.iwayee.activity.hub.Some;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class TestSystem extends BaseSystem {
  public void exec(Some some) {
    var act = some.getStr("case");
//    var act = ctx.pathParam("case");
    switch (act) {
      case "json":
        json(some);
        break;
      case "mysql":
        mysql(some);
        break;
      default:
        break;
    }
  }

  public void mysql(Some some) {
    var userDao = new UserDao();
    userDao.getUserByID(1, data -> {
      some.response().end("hello, mysql");
    });
  }

  public void json(Some some) {
    HttpServerResponse response = some.response();
    var member = new Member();
    member.id = 1;
    member.at = 123234233;
    member.pos = 0;

    List<Member> list = new ArrayList();
    list.add(member);
    list.add(member);

    JsonArray jam = new JsonArray();
    for (var m : list) {
      jam.add(JsonObject.mapFrom(m));
    }

//        String str = "{\n" +
//          "    \"nick\": \"嘿嘿\",\n" +
//          "    \"position\": 0,\n" +
//          "    \"player\": \"{\\\"id\\\":1,\\\"username\\\":\\\"haha\\\",\\\"wechat_nick\\\":\\\"huhu\\\",\\\"nick\\\":\\\"咔咔咔都\\\",\\\"sex\\\":1}\"\n" +
//          "}";
//        String str2 = "{\n" +
//          "    \"nick\": \"嘿嘿\",\n" +
//          "    \"position\": 0,\n" +
//          "    \"player\": {\n" +
//          "        \"id\": 1,\n" +
//          "        \"username\": \"haha\",\n" +
//          "        \"wechat_nick\": \"huhu\",\n" +
//          "        \"nick\": \"咔咔咔都\",\n" +
//          "        \"sex\": 1\n" +
//          "    }\n" +
//          "}";
//
//        Member m = new Member();
//        m.fromJson(new JsonObject(str2));

    var j = new JsonObject();
    try {
//          j.mapFrom(player);
      j.put("id", 2);
      j.put("player", member.toString());
//        var p = j.mapTo(Player.class);
//          ctx.json(j);
//          response.end(j.toString());
//          ctx.json(member.toJson());
      some.json(jam);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
//        var name = "jack";
//        var address = "荷光路";
//        ctx.json(
//          new JsonObject()
//            .put("name", name)
//            .put("address", address)
//            .put("message", "Hello " + name + " connected from " + address)
//        );
//        response.end("v2 is ok ");
  }
}
