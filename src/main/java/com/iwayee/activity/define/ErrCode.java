package com.iwayee.activity.define;

public enum ErrCode implements IErrorCode {
  SUCCESS(0, "成功"),

  ERR_PARAM(-100, "参数错误！"),
  ERR_DATA(-101, "数据错误！"),
  ERR_OP(-102, "操作失败！"),
  ERR_AUTH(-103, "登录验证失败，请重新登录！"),
  ERR_REGISTER(-104, "注册失败！"),
  ERR_USER_DATA(-105, "获取用户数据失败！"),
  ERR_USER_UPDATE_DATA(-106, "更新用户数据失败！"),

  ERR_GROUP_MANAGER_LIMIT(-200, "副群主数量超过限制，不能再委任！"),
  ERR_GROUP_GET_DATA(-201, "获取群数据失败！"),
  ERR_GROUP_APPROVE(-202, "入群审批失败！"),
  ERR_GROUP_UPDATE_OP(-203, "更新群信息失败！"),
  ERR_GROUP_NON_MANAGER(-204, "不是群管理员，没权限操作！"),
  ERR_GROUP_NON_OWNER(-205, "不是群主，没权限操作！"),
  ERR_GROUP_PROMOTE(-206, "委任副群主失败！"),
  ERR_GROUP_REMOVE(-207, "删除群成员失败！"),
  ERR_GROUP_TRANSFER(-208, "转让群主失败！"),
  ERR_GROUP_NON_MEMBER(-209, "不是群成员，没有权限操作！"),

  ERR_ACTIVITY_GET_DATA(-300, "获取活动数据失败！"),
  ERR_ACTIVITY_CANNOT_APPLY_NOT_IN_GROUP(-301, "你不是群组成员不能报名或取消报名群组活动！"),
  ERR_ACTIVITY_UPDATE(-302, "更新活动信息失败！"),
  ERR_ACTIVITY_NON_PLANNER(-303, "你不是活动发起人，没有权限操作！"),
  ERR_ACTIVITY_CREATE(-304, "创建新活动失败！"),
  ERR_ACTIVITY_FEE(-305, "选择活动前结算的活动，必须要填写费用！"),
  ERR_ACTIVITY_OVER_QUOTA(-306, "报名候补的数量超出限制，请稍后再报名！"),
  ERR_ACTIVITY_NOT_ENOUGH(-307, "取消报名的数量不正确！"),
  ERR_ACTIVITY_REMOVE(-308, "移除报名失败！"),
  ERR_ACTIVITY_NON_DOING(-309, "活动已经结束，不能再操作！"),
  ERR_ACTIVITY_CANNOT_CANCEL(-310, "取消报名的时间已过，不能取消报名！"),
  ERR_ACTIVITY_HAS_BEGUN(-311, "活动已经开始，不能报名！"),
  ;

  private int errorCode;
  private String errorDesc;

  ErrCode(int errorCode, String errorDesc) {
    this.errorCode = errorCode;
    this.errorDesc = errorDesc;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public String getErrorDesc() {
    return errorDesc;
  }
}
