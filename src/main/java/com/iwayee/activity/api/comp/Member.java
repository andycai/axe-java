package com.iwayee.activity.api.comp;

// 群组成员
public class Member {
  public long id;
  public int scores;
  public int pos; // 职位：1成员，2群主，3副群主
  public int sex;
  public long at; // 加入时间
  public String wx_nick;
  public String nick;

  public void fromUser(User user) {
    sex = user.sex;
    wx_nick = user.wx_nick;
    nick = user.nick;
  }
}
