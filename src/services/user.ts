import type { ApiResponse, PageResult } from '../types/common';
import type { User, UserCreateRequest, UserUpdateRequest } from '../types/user';
import api from './api';

/** 用户列表查询参数。 */
export interface UserListParams {
  page: number;
  size: number;
}

/**
 * 分页查询用户列表（仅管理员）。
 * @param params 分页参数
 * @returns 分页用户结果
 */
export async function listUsers(params: UserListParams): Promise<PageResult<User>> {
  const res = await api.get<ApiResponse<PageResult<User>>>('/users', { params });
  return res.data.data;
}

/**
 * 添加用户（仅管理员）。
 * @param data 添加用户请求
 * @returns 新增的用户
 */
export async function createUser(data: UserCreateRequest): Promise<User> {
  const res = await api.post<ApiResponse<User>>('/users', data);
  return res.data.data;
}

/**
 * 修改用户（仅管理员），支持修改显示名/用户类型/密码/登录开关。
 * 禁止登录传 { enabled: 0 }，恢复登录传 { enabled: 1 }。
 * @param id 用户 ID
 * @param data 修改字段
 * @returns 修改后的用户
 */
export async function updateUser(id: string, data: UserUpdateRequest): Promise<User> {
  const res = await api.put<ApiResponse<User>>(`/users/${id}`, data);
  return res.data.data;
}
