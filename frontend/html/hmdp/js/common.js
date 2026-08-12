// let commonURL = "http://192.168.50.115:8081";
let commonURL = "/api";
// 设置后台服务地址
axios.defaults.baseURL = commonURL;
axios.defaults.timeout = 5000;
// request拦截器，将用户token放入头中
let token = sessionStorage.getItem("token");
axios.interceptors.request.use(
  config => {
    if(token) config.headers['authorization'] = token
    return config
  },
  error => {
    console.log(error)
    return Promise.reject(error)
  }
)
axios.interceptors.response.use(function (response) {
  // 判断执行结果
  if (!response.data.success) {
    return Promise.reject(response.data.errorMsg || "操作失败")
  }
  return response.data;
}, function (error) {
  // 一般是服务端异常或者网络异常
  console.log(error)
  // 请求超时
  if (error.code === 'ECONNABORTED' || /^timeout of /.test(error.message || '')) {
    return Promise.reject("请求超时，请检查网络后重试");
  }
  // 没有响应体，说明网络不通或服务未启动
  if (!error.response) {
    return Promise.reject("网络异常，无法连接服务器");
  }
  const status = error.response.status;
  // 优先透出后端返回的具体错误信息
  try {
    let data = error.response.data;
    // 有明确errorMsg的是具体业务/接口错误，直接展示；无errorMsg的401才是未登录
    if (data && data.errorMsg) {
      return Promise.reject(data.errorMsg);
    }
  } catch(e) {}
  // 未登录，记录来源页后跳转（登录成功后可返回原页面，对齐主流App体验）
  if (status == 401) {
    sessionStorage.setItem("login_from", location.pathname + location.search);
    setTimeout(() => {
      location.href = "/login.html"
    }, 200);
    return Promise.reject("请先登录");
  }
  // 按状态码给出具体提示
  const statusMsg = {
    400: "请求参数错误",
    404: "请求的接口不存在",
    405: "请求方法不被允许",
    415: "请求格式不支持",
    500: "服务器内部错误，请稍后重试",
    502: "网关错误，请稍后重试",
    503: "服务暂不可用，请稍后重试"
  };
  return Promise.reject(statusMsg[status] || ("服务器异常（HTTP " + status + "）"));
});
axios.defaults.paramsSerializer = function(params) {
  let p = "";
  Object.keys(params).forEach(k => {
    if(params[k]){
      p = p + "&" + k + "=" + params[k]
    }
  })
  return p;
}
const util = {
  commonURL,
  /**
   * 兼容后端多种时间格式（ISO字符串/时间戳/数组）转毫秒
   */
  parseTime(t) {
    if (!t) return 0;
    if (typeof t === "number") return t;
    if (Array.isArray(t)) {
      // LocalDateTime 无 jsr310 时序列化为 [y,m,d,h,mi,s]
      return new Date(t[0], (t[1] || 1) - 1, t[2] || 1, t[3] || 0, t[4] || 0, t[5] || 0).getTime();
    }
    return new Date(t).getTime();
  },
  /**
   * 相对时间展示（对齐主流App：刚刚/x分钟前/x小时前/昨天/MM月DD日）
   */
  relativeTime(t) {
    let ts = util.parseTime(t);
    if (!ts) return "";
    let diff = Date.now() - ts;
    if (diff < 60000) return "刚刚";
    if (diff < 3600000) return Math.floor(diff / 60000) + "分钟前";
    if (diff < 86400000) return Math.floor(diff / 3600000) + "小时前";
    if (diff < 172800000) return "昨天";
    let d = new Date(ts);
    return (d.getMonth() + 1) + "月" + d.getDate() + "日";
  },
  /**
   * 登录成功后的跳转目标：优先返回触发登录的来源页，否则回首页
   */
  loginRedirectUrl() {
    const from = sessionStorage.getItem("login_from");
    sessionStorage.removeItem("login_from");
    if (from && from.indexOf("/login") !== 0) {
      return from;
    }
    return "/index.html";
  },
  getUrlParam(name) {
    let reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    let r = window.location.search.substr(1).match(reg);
    if (r != null) {
      return decodeURI(r[2]);
    }
    return "";
  },
  formatPrice(val) {
    if (typeof val === 'string') {
      if (isNaN(val)) {
        return null;
      }
      // 价格转为整数
      const index = val.lastIndexOf(".");
      let p = "";
      if (index < 0) {
        // 无小数
        p = val + "00";
      } else if (index === val.length - 2) {
        // 1位小数
        p = val.replace(".", "") + "0";
      } else {
        // 2位小数
        p = val.replace(".", "")
      }
      return parseInt(p);
    } else if (typeof val === 'number') {
      if (!val) {
        return null;
      }
      const s = val + '';
      if (s.length === 0) {
        return "0.00";
      }
      if (s.length === 1) {
        return "0.0" + val;
      }
      if (s.length === 2) {
        return "0." + val;
      }
      const i = s.indexOf(".");
      if (i < 0) {
        return s.substring(0, s.length - 2) + "." + s.substring(s.length - 2)
      }
      const num = s.substring(0, i) + s.substring(i + 1);
      if (i === 1) {
        // 1位整数
        return "0.0" + num;
      }
      if (i === 2) {
        return "0." + num;
      }
      if (i > 2) {
        return num.substring(0, i - 2) + "." + num.substring(i - 2)
      }
    }
  }
}
