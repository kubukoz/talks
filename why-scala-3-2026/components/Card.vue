<script setup lang="ts">
defineProps<{
  title?: string
  number?: string | number
  dark?: boolean
  compact?: boolean
}>()
</script>

<template>
  <div class="card" :class="{ dark, compact }">
    <p v-if="number !== undefined" class="card-number">{{ number }}</p>
    <h3 v-if="title">{{ title }}</h3>
    <slot />
  </div>
</template>
