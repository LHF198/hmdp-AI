import http from './http'

// 消息中心相关接口
export const messageApi = {
  // 评论我的消息
  comments: () => http.get('/message/comments'),
  // 关注我的消息
  follows: () => http.get('/message/follows'),
}
