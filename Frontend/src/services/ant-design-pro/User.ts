// @ts-ignore
/* eslint-disable */
import {request} from '@umijs/max';

/** Create a new user POST /api/user/create */
export async function create(body: API.CreateUserData, options?: { [key: string]: any }) {
  return request<API.ResponseStructure>('/api/user/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** Get user info GET /api/user/current */
export async function current(options?: { [key: string]: any }) {
  return request<API.ResponseStructure>('/api/user/current', {
    method: 'GET',
    ...(options || {}),
  });
}

/** Login POST /api/user/login */
export async function login(body: API.LoginData, options?: { [key: string]: any }) {
  return request<API.ResponseStructure>('/api/user/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** Logout GET /api/user/logout */
export async function logout(options?: { [key: string]: any }) {
  return request<API.ResponseStructure>('/api/user/logout', {
    method: 'GET',
    ...(options || {}),
  });
}
