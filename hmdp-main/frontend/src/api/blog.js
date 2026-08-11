import http from './http'

// 笔记相关接口（与后端 BlogController / UploadController 对齐）
export const blogApi = {
  // 热门笔记分页（首页瀑布流）
  hot: (current) => http.get('/blog/hot', { params: { current } }),
  // 点赞/取消点赞
  like: (id) => http.put('/blog/like/' + id),
  // 笔记详情（点赞后刷新状态用）
  get: (id) => http.get('/blog/' + id),
  // 某用户的笔记列表
  ofUser: (params) => http.get('/blog/of/user', { params }),
  // 发布笔记
  publish: (data) => http.post('/blog', data),
  // 上传图片（FormData），返回相对路径 /blogs/xx/xx/xxx.jpg
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/upload/blog', fd)
  },
  // 删除已上传但未使用的图片（name 为完整 /imgs 前缀路径）
  deleteImage: (name) => http.get('/upload/blog/delete', { params: { name } }),
  // 点赞用户列表（笔记详情页）
  likes: (id) => http.get('/blog/likes/' + id),
  // 我的笔记（个人主页）
  ofMe: (current) => http.get('/blog/of/me', { params: { current } }),
  // 关注的人的最新笔记（滚动 feed，lastId/offset 游标）
  ofFollow: (params) => http.get('/blog/of/follow', { params }),
  // 删除笔记（仅作者）
  del: (id) => http.delete('/blog/' + id),
}

// 笔记评论接口（与后端 BlogCommentsController 对齐）
export const commentApi = {
  // 分页查询笔记评论（records + total）
  list: (blogId, current) => http.get('/blog-comments/' + blogId, { params: { current } }),
  // 发布评论
  add: (blogId, content) => http.post('/blog-comments/' + blogId, { content }),
  // 删除评论（仅评论作者本人）
  remove: (commentId) => http.delete('/blog-comments/' + commentId),
  // 我发出的评论（个人主页“评价”tab，含关联笔记标题）
  ofMe: () => http.get('/blog-comments/of/me'),
}
