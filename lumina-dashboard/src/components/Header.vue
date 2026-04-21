<template>
  <header class="lumina-shell-header sticky top-0 z-50">
    <div class="container mx-auto px-4 py-5">
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-3">
          <div class="brand-mark w-11 h-11 rounded-full flex items-center justify-center">
            <svg class="w-6 h-6 text-[#163300]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <div>
            <h1 class="text-2xl font-black tracking-tight text-[#0e0f0c]">Lumina-RPC</h1>
            <p class="text-xs text-[#454745]">Observable RPC without borders</p>
          </div>
        </div>

        <nav class="hidden md:flex items-center space-x-1">
          <RouterLink
            to="/"
            class="nav-link px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="route.path === '/' ? 'nav-link-active' : 'nav-link-idle'"
          >
            大盘看板
          </RouterLink>
          <RouterLink
            to="/services"
            class="nav-link px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="route.path === '/services' ? 'nav-link-active' : 'nav-link-idle'"
          >
            服务管理
          </RouterLink>
          <RouterLink
            to="/mock-rules"
            class="nav-link px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="route.path === '/mock-rules' ? 'nav-link-active' : 'nav-link-idle'"
          >
            Mock 规则
          </RouterLink>
          <RouterLink
            to="/protection"
            class="nav-link px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="route.path === '/protection' ? 'nav-link-active' : 'nav-link-idle'"
          >
            服务保护
          </RouterLink>
          <RouterLink
            to="/consumer-ops"
            class="nav-link px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center space-x-1"
            :class="route.path === '/consumer-ops' ? 'nav-link-active' : 'nav-link-idle'"
          >
            <span>📊</span>
            <span>消费者操作台</span>
          </RouterLink>
          <RouterLink
            to="/traces"
            class="nav-link px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center space-x-1"
            :class="route.path === '/traces' ? 'nav-link-active' : 'nav-link-idle'"
          >
            <span>🔗</span>
            <span>链路追踪</span>
          </RouterLink>
        </nav>

        <div class="flex items-center space-x-4">
          <div class="status-pill text-xs hidden sm:block">
            <span class="flex items-center space-x-2">
              <span class="w-2 h-2 bg-[#054d28] rounded-full animate-pulse"></span>
              <span>Control Plane 运行中</span>
            </span>
          </div>

          <div class="md:hidden">
            <button class="p-2 text-[#0e0f0c] transition-colors">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { RouterLink } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
header {
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.88), rgba(246, 247, 242, 0.86)),
    radial-gradient(circle at 12% 0%, rgba(159, 232, 112, 0.38), transparent 28rem);
  border-bottom: 1px solid rgba(14, 15, 12, 0.12);
  box-shadow: rgba(14, 15, 12, 0.12) 0 0 0 1px;
  backdrop-filter: blur(18px);
}

.brand-mark {
  background: #9fe870;
  border: 1px solid rgba(14, 15, 12, 0.12);
  box-shadow: rgba(14, 15, 12, 0.12) 0 0 0 1px;
}

.nav-link {
  border: 1px solid transparent;
}

.nav-link-idle {
  color: #454745;
}

.nav-link:hover,
.nav-link-active {
  color: #163300;
  background: rgba(211, 242, 192, 0.5);
  border-color: rgba(14, 15, 12, 0.12);
}

.status-pill {
  color: #163300;
  background: rgba(226, 246, 213, 0.9);
  border: 1px solid rgba(14, 15, 12, 0.12);
  border-radius: 9999px;
  padding: 8px 12px;
}

nav a {
  position: relative;
}

nav a::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #163300, #9fe870);
  transition: width 0.3s ease, left 0.3s ease;
}

nav a:hover::after {
  width: 100%;
  left: 0;
}
</style>
