<template>
  <div id="app" class="min-h-screen">
    <Header />

    <main class="container mx-auto px-4 py-6">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import Header from './components/Header.vue'
</script>

<style scoped>
#app {
  font-family: 'Inter', Helvetica, Arial, sans-serif;
  font-feature-settings: "calt" 1;
}
</style>
