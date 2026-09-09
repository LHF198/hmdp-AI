<template>
  <!-- 图片轮播（触摸滑动 + 鼠标拖拽） -->
  <div
    class="blog-info-box"
    ref="swiper"
    :style="swiperHeight ? { height: swiperHeight + 'px' } : {}"
    @touchstart="moveStart"
    @touchmove="moving"
    @touchend="moveEnd"
    @mousedown="mouseDown"
    @mousemove="mouseMoving"
    @mouseup="mouseEnd"
    @mouseleave="mouseEnd"
  >
    <div class="swiper-item" v-for="(img, i) in images" :key="i">
      <img :src="img" alt="" draggable="false" @load="onImgLoad(i, $event)" />
    </div>
    <div class="indicator-dot" v-if="images && images.length > 1">
      {{ active + 1 }}/{{ images.length }}
    </div>
    <!-- 左右切换箭头（多张图时显示） -->
    <div
      v-if="images && images.length > 1"
      class="swiper-arrow swiper-prev"
      @click.stop="prevSlide"
    >
      <el-icon :size="20"><ArrowLeft /></el-icon>
    </div>
    <div
      v-if="images && images.length > 1"
      class="swiper-arrow swiper-next"
      @click.stop="nextSlide"
    >
      <el-icon :size="20"><ArrowRight /></el-icon>
    </div>
  </div>
</template>

<script setup>
// 笔记图片轮播：触摸滑动 + 鼠标拖拽 + 随图比例自适应高度（迁移自旧 blog-detail.html 的手写 swiper）
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  images: { type: Array, default: () => [] },
})

// 触摸轮播状态
const swiper = ref(null)
const _width = ref(0)
const items = ref([])
const active = ref(0)
const imgRatios = ref({}) // 每张图的自然宽高比（width / height）
const swiperHeight = ref(0) // 轮播容器高度：随当前图比例自适应，保证图片尽量完整展示
const duration = 300
const sensitivity = 60
const resistance = 0.3
const start = { x: 0, y: 0 }
const move = { x: 0, y: 0 }
let isMoving = false
let isMouseDragging = false
let mouseMoved = false // 鼠标是否真实拖动过（区分点击与拖拽）
let lastTouchTime = 0 // 触屏点击会合成鼠标事件，用于去重

// 图片加载完成后初始化轮播（宽度由 CSS 决定，无需等图片加载）
watch(
  () => props.images,
  (imgs) => {
    if (imgs && imgs.length) {
      nextTick(() => {
        initSwiper()
        // 延迟再次初始化，确保 DOM 渲染完成（解决首次 _width 为 0 的问题）
        setTimeout(() => initSwiper(), 100)
      })
    }
  }
)

// 图片加载完成后记录自然比例；若是当前展示图则同步调整容器高度
function onImgLoad(i, e) {
  const img = e.target
  if (img.naturalWidth && img.naturalHeight) {
    imgRatios.value[i] = img.naturalWidth / img.naturalHeight
    if (i === active.value) {
      applySwiperHeight()
    }
  }
}

// 按当前图比例自适应轮播高度：最小 200px，最大 75vh（防极长图撑爆页面）
function applySwiperHeight() {
  const ratio = imgRatios.value[active.value]
  if (!ratio || !swiper.value) return
  const w = swiper.value.offsetWidth
  if (!w) return
  const h = w / ratio
  swiperHeight.value = Math.round(Math.min(Math.max(h, 200), window.innerHeight * 0.75))
}

function initSwiper() {
  if (!swiper.value) return
  items.value = swiper.value.querySelectorAll('.swiper-item')
  const w = swiper.value.offsetWidth || document.documentElement.offsetWidth
  if (w > 0) {
    _width.value = w
  }
  // 如果 _width 仍为 0，无法正确布局，直接返回等下次重试
  if (_width.value <= 0) return
  setTransform()
  setTransition('none')
}

function setTransform(offset) {
  offset = offset || 0
  items.value.forEach((item, i) => {
    const distance = (i - active.value) * _width.value + offset
    const transform = `translate3d(${distance}px, 0, 0)`
    item.style.webkitTransform = transform
    item.style.transform = transform
  })
}

function setTransition(d) {
  d = d || duration
  d = typeof d === 'number' ? d + 'ms' : d
  items.value.forEach((item) => {
    item.style.webkitTransition = d
    item.style.transition = d
  })
}

function moveStart(e) {
  // 点在箭头按钮上时不进入拖拽逻辑，交给箭头的 click 处理
  if (e.target.closest && e.target.closest('.swiper-arrow')) return
  lastTouchTime = Date.now()
  start.x = e.changedTouches[0].pageX
  start.y = e.changedTouches[0].pageY
  setTransition('none')
}

function moving(e) {
  e.preventDefault()
  e.stopPropagation()
  const distanceX = e.changedTouches[0].pageX - start.x
  const distanceY = e.changedTouches[0].pageY - start.y
  if (Math.abs(distanceX) > Math.abs(distanceY)) {
    isMoving = true
    move.x = start.x + distanceX
    move.y = start.y + distanceY
    // 首项右滑/末项左滑加阻力，形成拉弹簧效果
    let dx = distanceX
    if (
      (active.value === 0 && distanceX > 0) ||
      (active.value === items.value.length - 1 && distanceX < 0)
    ) {
      dx = distanceX * resistance
    }
    setTransform(dx)
  }
}

function moveEnd(e) {
  if (isMoving) {
    e.preventDefault()
    e.stopPropagation()
    const distance = move.x - start.x
    if (Math.abs(distance) > sensitivity) {
      if (distance < 0) {
        nextSlide()
      } else {
        prevSlide()
      }
    } else {
      back()
    }
    reset()
    isMoving = false
  }
}

// 鼠标拖拽支持（桌面端）
function mouseDown(e) {
  if (e.button !== 0) return // 仅左键
  // 点在箭头按钮上时不启动拖拽，避免 mouseup 误判为滑动导致跳页
  if (e.target.closest && e.target.closest('.swiper-arrow')) return
  // 触摸操作后的合成鼠标事件跳过，防止一次手势触发两次切换
  if (Date.now() - lastTouchTime < 500) return
  isMouseDragging = true
  mouseMoved = false
  start.x = e.pageX
  start.y = e.pageY
  setTransition('none')
}

function mouseMoving(e) {
  if (!isMouseDragging) return
  const distanceX = e.pageX - start.x
  const distanceY = e.pageY - start.y
  if (Math.abs(distanceX) > Math.abs(distanceY)) {
    e.preventDefault()
    mouseMoved = true
    move.x = e.pageX
    move.y = e.pageY
    let dx = distanceX
    if (
      (active.value === 0 && distanceX > 0) ||
      (active.value === items.value.length - 1 && distanceX < 0)
    ) {
      dx = distanceX * resistance
    }
    setTransform(dx)
  }
}

function mouseEnd(e) {
  if (!isMouseDragging) return
  isMouseDragging = false
  // 未发生真实位移（纯点击/误触）只做回弹，避免把点击误判为滑动
  if (!mouseMoved) {
    back()
    return
  }
  const distance = move.x - start.x
  if (Math.abs(distance) > sensitivity) {
    if (distance < 0) {
      nextSlide()
    } else {
      prevSlide()
    }
  } else {
    back()
  }
  reset()
}

function nextSlide() {
  go(active.value + 1)
}

function prevSlide() {
  go(active.value - 1)
}

function reset() {
  start.x = 0
  start.y = 0
  move.x = 0
  move.y = 0
}

function back() {
  setTransition()
  setTransform()
}

function go(index) {
  active.value = index
  if (active.value < 0) {
    active.value = 0
  } else if (active.value > items.value.length - 1) {
    active.value = items.value.length - 1
  }
  setTransition()
  setTransform()
  // 切换后按新图比例调整容器高度（该图未加载完成时维持原高度，load 后再修正）
  applySwiperHeight()
}
</script>
