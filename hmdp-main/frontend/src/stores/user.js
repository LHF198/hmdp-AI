import { defineStore } from 'pinia'

// 用户状态：token 与 sessionStorage 保持同步（旧页面共用同一存储，双入口互通）
export const useUserStore = defineStore('user', {
  state: () => ({
    token: sessionStorage.getItem('token') || '',
    userInfo: null,
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
  },
  actions: {
    setToken(token) {
      this.token = token || ''
      if (token) {
        sessionStorage.setItem('token', token)
      } else {
        sessionStorage.removeItem('token')
      }
    },
    logout() {
      this.setToken('')
      this.userInfo = null
    },
  },
})
