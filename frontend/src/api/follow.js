import http from './http'

// 关注相关接口（与后端 FollowController 对齐）
export const followApi = {
  // 当前登录用户是否关注了目标用户
  orNot: (id) => http.get('/follow/or/not/' + id),
  // 与目标用户的共同关注列表
  common: (id) => http.get('/follow/common/' + id),
  // 关注（true）/ 取消关注（false）
  follow: (id, followed) => http.put(`/follow/${id}/${followed}`),
}
