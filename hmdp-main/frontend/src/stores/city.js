import { defineStore } from 'pinia'

// 城市状态：localStorage 键名与旧页面一致（hmdp_city），跨 SPA/旧 MPA 共享
export const useCityStore = defineStore('city', {
  state: () => ({
    city: localStorage.getItem('hmdp_city') || '',
  }),
  actions: {
    setCity(city) {
      this.city = city
      if (city) {
        localStorage.setItem('hmdp_city', city)
      } else {
        localStorage.removeItem('hmdp_city')
      }
    },
  },
})
