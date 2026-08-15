import type { ApiResponse } from '../types/common';
import type { LoginRequest, User } from '../types/user';
import api from './api';

/** localStorage 中保存当前登录用户的键名。 */
const CURRENT_USER_KEY = 'currentUser';

/**
 * 登录：调用 POST /api/auth/login，成功后后端写入 HttpOnly Cookie，
 * 并将返回的用户信息保存到 localStorage 供前端判断当前用户。
 * @param data 登录请求（登录名 + 密码）
 * @returns 当前登录用户
 */
export async function login(data: LoginRequest): Promise<User> {
  const res = await api.post<ApiResponse<User>>('/auth/login', data);
  const user = res.data.data;
  localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
  return user;
}

/**
 * 获取本地保存的当前登录用户。
 * @returns 当前登录用户，未登录或数据损坏时返回 null
 */
export function getCurrentUser(): User | null {
  const raw = localStorage.getItem(CURRENT_USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

/**
 * 清除本地保存的当前登录用户（退出登录时调用）。
 */
export function clearCurrentUser(): void {
  localStorage.removeItem(CURRENT_USER_KEY);
}
