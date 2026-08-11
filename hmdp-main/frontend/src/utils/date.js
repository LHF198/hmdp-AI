// 时间工具：从旧 common.js 的 util 迁移，行为保持一致

/**
 * 兼容后端多种时间格式（ISO字符串/时间戳/数组）转毫秒
 */
export function parseTime(t) {
  if (!t) return 0
  if (typeof t === 'number') return t
  if (Array.isArray(t)) {
    // LocalDateTime 无 jsr310 时序列化为 [y,m,d,h,mi,s]
    return new Date(t[0], (t[1] || 1) - 1, t[2] || 1, t[3] || 0, t[4] || 0, t[5] || 0).getTime()
  }
  return new Date(t).getTime()
}

/**
 * 相对时间展示（对齐主流App：刚刚/x分钟前/x小时前/昨天/MM月DD日）
 */
export function relativeTime(t) {
  const ts = parseTime(t)
  if (!ts) return ''
  const diff = Date.now() - ts
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 172800000) return '昨天'
  const d = new Date(ts)
  return (d.getMonth() + 1) + '月' + d.getDate() + '日'
}

/**
 * 固定格式时间（消息页用：yyyy-MM-dd HH:mm）
 */
export function formatTime(t) {
  if (!t) return ''
  // 兼容 "2022-01-05T12:00:00" 或数据库返回的 "2022-01-05 12:00:00"
  const s = String(t).replace('T', ' ').split('.')[0]
  return s.substring(0, 16)
}

/**
 * 登录成功后的跳转目标：优先返回触发登录的来源页，否则回 SPA 首页
 */
export function loginRedirectUrl() {
  const from = sessionStorage.getItem('login_from')
  sessionStorage.removeItem('login_from')
  if (from && !from.includes('/login')) {
    return from
  }
  return import.meta.env.BASE_URL
}
