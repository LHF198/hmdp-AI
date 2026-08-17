<template>
  <!-- Teleport 到 body：绕开 #app 的 backdrop-filter 包含块陷阱（同 FootBar），
       fixed 定位相对视口而非 #app，避免内容超高时按钮被推到视口外 -->
  <Teleport to="body">
    <div class="ai-launcher" @click="open">
      <div class="ai-launcher-button" title="打开AI顾问">
        <svg viewBox="0 0 48 48" aria-hidden="true">
          <defs>
            <linearGradient id="ai-icon-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#fff1ea"></stop>
              <stop offset="100%" stop-color="#ffd9c7"></stop>
            </linearGradient>
          </defs>
          <circle cx="24" cy="24" r="18" fill="rgba(255,255,255,0.22)"></circle>
          <path
            d="M17 18c0-3.314 2.686-6 6-6h2c3.314 0 6 2.686 6 6 0 1.824-.82 3.453-2.108 4.551-.26.221-.26.675 0 .896C29.18 24.545 30 26.175 30 28c0 3.314-2.686 6-6 6h-2c-3.314 0-6-2.686-6-6 0-1.825.82-3.455 2.108-4.553.26-.221.26-.675 0-.896C17.82 21.454 17 19.824 17 18Z"
            stroke="url(#ai-icon-gradient)"
            stroke-width="2.6"
            stroke-linecap="round"
            stroke-linejoin="round"
            fill="none"
          ></path>
          <circle cx="20" cy="18" r="1.8" fill="url(#ai-icon-gradient)"></circle>
          <circle cx="28" cy="18" r="1.8" fill="url(#ai-icon-gradient)"></circle>
          <path d="M20 28h8" stroke="url(#ai-icon-gradient)" stroke-width="2.6" stroke-linecap="round"></path>
        </svg>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { useRouter } from 'vue-router'

// AI 顾问悬浮按钮：从旧 shop-list/blog-edit 页面的 ai-launcher 迁移，SPA 内跳转 /ai
const router = useRouter()
function open() {
  router.push('/ai')
}
</script>

<style>
/* 悬浮按钮：统一为右下角 FAB（旧实现在桌面端垂直居中右侧、移动端右下，两端行为割裂）。
   组件已 Teleport 到 body，此处 fixed 相对视口定位；
   bottom 避让底部导航（--foot-height + --foot-offset）。 */
.ai-launcher {
  position: fixed;
  right: var(--space-6);
  bottom: calc(var(--foot-height) + var(--foot-offset) + var(--space-5));
  z-index: var(--z-launcher);
  pointer-events: auto;
  cursor: pointer;
}
.ai-launcher-button {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff7a45, var(--brand-strong));
  box-shadow: 0 14px 30px rgba(180, 90, 50, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  color: var(--brand-on);
  transition: transform var(--dur-base) ease, box-shadow var(--dur-base) ease,
    filter var(--dur-base) ease;
}
.ai-launcher-button:hover {
  transform: translateY(-2px) scale(1.04);
  box-shadow: 0 18px 38px rgba(180, 90, 50, 0.4);
  filter: brightness(1.05);
}
.ai-launcher-button:active {
  transform: translateY(0) scale(0.97);
}
.ai-launcher-button svg {
  width: 28px;
  height: 28px;
}
</style>
