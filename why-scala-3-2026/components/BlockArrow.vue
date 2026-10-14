<script setup lang="ts">
withDefaults(defineProps<{ width?: number }>(), { width: 64 })
</script>

<template>
  <svg class="arrow" :width="width" :height="width / 2" viewBox="0 0 64 32" aria-hidden="true">
    <path d="M0 10 H38 V0 L64 16 L38 32 V22 H0 Z" fill="var(--sc-red)" />
  </svg>
</template>
