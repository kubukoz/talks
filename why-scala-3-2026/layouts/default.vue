<script setup lang="ts">
defineProps<{ eyebrow?: string }>()
</script>

<template>
  <div class="slidev-layout sc default">
    <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
    <slot />
    <Logo />
  </div>
</template>
