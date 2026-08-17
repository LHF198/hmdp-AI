<template>
  <!-- 根组件：路由出口 + 页面切换过渡（过渡类定义见 styles/components.css .page-fade-*） -->
  <router-view v-slot="{ Component, route }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
</template>

<script setup>
// 根组件：仅承载路由出口，页面级样式由各页面/全局样式负责
</script>
