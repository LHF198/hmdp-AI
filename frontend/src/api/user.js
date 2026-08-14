import http from './http'

// 用户相关接口（与后端 UserController 对齐）
export const userApi = {
  // 手机号+验证码 / 账号密码登录，返回 token 字符串
  login: (form) => http.post('/user/login', form),
  // 发送验证码（演示环境后端直接回显验证码）
  sendCode: (phone) => http.post('/user/code?phone=' + phone),
  logout: () => http.post('/user/logout'),
  // 当前登录用户
  me: () => http.get('/user/me'),
  // 用户公开信息（他人主页）
  getUser: (id) => http.get('/user/' + id),
  // 用户详情（含手机号等隐私字段，仅本人）
  getUserInfo: (id) => http.get('/user/info/' + id),
  // 签到
  sign: () => http.post('/user/sign'),
  signCount: () => http.get('/user/sign/count'),
  updateInfo: (data) => http.put('/user/info', data),
  // 设置/修改密码（{ oldPassword?, newPassword }，未设置过密码可省略 oldPassword）
  setPassword: (data) => http.put('/user/password', data),
}
