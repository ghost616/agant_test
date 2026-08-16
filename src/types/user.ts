/**
 * 用户相关类型定义。
 */

/** 用户类型：普通用户。 */
export const USER_TYPE_NORMAL = 1;

/** 用户类型：管理员。 */
export const USER_TYPE_ADMIN = 2;

export interface User {
  id: string;
  loginName: string;
  displayName?: string;
  /** 用户类型：1 普通用户，2 管理员。 */
  userType: number;
  /** 登录开关：0 禁止登录，1 允许登录。 */
  enabled: number;
  createTime?: string;
  updateTime?: string;
}

export interface LoginRequest {
  loginName: string;
  password: string;
}

export interface UserCreateRequest {
  loginName: string;
  displayName?: string;
  password: string;
  enabled?: number;
}

export interface UserUpdateRequest {
  displayName?: string;
  password?: string;
  enabled?: number;
}

/** 当前登录用户自助修改请求（显示名/密码，字段为空不修改，enabled 不可自助修改）。 */
export interface UserSelfUpdateRequest {
  displayName?: string;
  password?: string;
}
