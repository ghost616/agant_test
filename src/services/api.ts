import axios from 'axios';
import type { ApiResponse } from '../types/common';

/** 未登录错误码，响应拦截器命中时跳转登录页。 */
const USER_NOT_LOGIN_CODE = 'USER-NOT-LOGIN';

/** localStorage 中保存当前登录用户的键名（与 auth.ts 保持一致）。 */
const CURRENT_USER_KEY = 'currentUser';

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * 清理本地登录状态并跳转登录页（避免循环依赖，不引用 auth.ts）。
 */
function redirectToLogin(): void {
  localStorage.removeItem(CURRENT_USER_KEY);
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

api.interceptors.request.use(
  (config) => {
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

api.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>;
    if (body && !body.success) {
      if (body.code === USER_NOT_LOGIN_CODE) {
        redirectToLogin();
      }
      return Promise.reject(new Error(body.message || '请求失败'));
    }
    return response;
  },
  (error) => {
    const body = error.response?.data as ApiResponse<unknown> | undefined;
    if (body?.code === USER_NOT_LOGIN_CODE) {
      redirectToLogin();
    }
    const message = body?.message || error.message || '网络异常';
    return Promise.reject(new Error(message));
  },
);

export default api;
