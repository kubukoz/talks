<script setup lang="ts">
defineProps<{ number?: string | number }>()
</script>

<template>
  <div class="slidev-layout sc dark section">
    <p v-if="number !== undefined" class="section-number">{{ number }}</p>
    <slot />
  </div>
</template>
