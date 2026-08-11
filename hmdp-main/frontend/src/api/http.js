import axios from 'axios'

// 由 main.js 注入 router 实例（避免 api 层与 router 循环依赖）
let _router = null
export function setRouter(router) {
  _router = router
}

const http = axios.create({
  baseURL: '/api',
  timeout: 5000,
})

// request 拦截器：将用户 token 放入请求头（与旧 common.js 行为一致）
http.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('token')
    if (token) config.headers['authorization'] = token
    return config
  },
  (error) => {
    console.log(error)
    return Promise.reject(error)
  },
)

// response 拦截器：解包 Result（{success,data,errorMsg}），401 记录来源页并跳登录
http.interceptors.response.use(
  function (response) {
    // 判断执行结果
    if (!response.data.success) {
      return Promise.reject(response.data.errorMsg || '操作失败')
    }
    return response.data
  },
  function (error) {
    // 一般是服务端异常或者网络异常
    console.log(error)
    // 请求超时
    if (error.code === 'ECONNABORTED' || /^timeout of /.test(error.message || '')) {
      return Promise.reject('请求超时，请检查网络后重试')
    }
    // 没有响应体，说明网络不通或服务未启动
    if (!error.response) {
      return Promise.reject('网络异常，无法连接服务器')
    }
    const status = error.response.status
    // 优先透出后端返回的具体错误信息
    try {
      const data = error.response.data
      // 有明确 errorMsg 的是具体业务/接口错误，直接展示；无 errorMsg 的 401 才是未登录
      if (data && data.errorMsg) {
        return Promise.reject(data.errorMsg)
      }
    } catch (e) {}
    // 未登录，记录来源页后跳转（登录成功后可返回原页面，对齐主流 App 体验）
    if (status === 401) {
      sessionStorage.setItem('login_from', location.pathname + location.search)
      setTimeout(() => {
        if (_router) _router.push('/login')
      }, 200)
      return Promise.reject('请先登录')
    }
    // 按状态码给出具体提示
    const statusMsg = {
      400: '请求参数错误',
      404: '请求的接口不存在',
      405: '请求方法不被允许',
      415: '请求格式不支持',
      500: '服务器内部错误，请稍后重试',
      502: '网关错误，请稍后重试',
      503: '服务暂不可用，请稍后重试',
    }
    return Promise.reject(statusMsg[status] || ('服务器异常（HTTP ' + status + '）'))
  },
)

// 与旧 common.js 一致的 params 序列化：过滤空值参数
http.defaults.paramsSerializer = function (params) {
  let p = ''
  Object.keys(params).forEach((k) => {
    if (params[k]) {
      p = p + '&' + k + '=' + params[k]
    }
  })
  return p
}

export default http
