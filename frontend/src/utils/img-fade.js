/**
 * v-img-fade：图片加载完成后渐显，避免列表滚动时图片"硬弹出"。
 *
 * 用法：<img v-img-fade :src="url" />
 * 样式：styles/components.css 的 .img-fade / .img-fade.is-loaded
 *
 * 说明：缓存命中时图片可能在指令挂载前就已 complete，此处先判断 complete 再挂监听，
 * 否则 load 事件已经过去、图片会永久停在 opacity:0。
 */
const reveal = (el) => el.classList.add('is-loaded')

export const imgFade = {
  mounted(el) {
    el.classList.add('img-fade')
    // 已完成（含缓存命中/同步解码）直接显示
    if (el.complete && el.naturalWidth > 0) {
      reveal(el)
      return
    }
    el.addEventListener('load', () => reveal(el), { once: true })
    // 加载失败也要显示，避免破图位置留空白
    el.addEventListener('error', () => reveal(el), { once: true })
  },
  // src 变化（如列表复用节点）时重新进入渐显
  updated(el) {
    if (el.complete && el.naturalWidth > 0) {
      reveal(el)
    } else {
      el.classList.remove('is-loaded')
    }
  },
}

export default imgFade
