<script setup lang="ts">
// A slide that is just one full-bleed image, e.g. copied verbatim from another deck.
defineProps<{ image: string }>()
</script>

<template>
  <div class="slidev-layout sc picture">
    <img :src="image" alt="" />
    <slot />
  </div>
</template>
