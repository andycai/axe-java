package com.iwayee.activity.define;

// 结算方式:1免费,2活动前,3活动后男女平均,4活动后男固定|女平摊,5活动后男平摊|女固定
public enum ActivityFeeType {
  NONE,
  FEE_TYPE_FREE,
  FEE_TYPE_BEFORE,
  FEE_TYPE_AFTER_AA,
  FEE_TYPE_AFTER_AB,
  FEE_TYPE_AFTER_BA
}
