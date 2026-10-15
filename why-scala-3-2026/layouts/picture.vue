<script setup lang="ts">
// A slide that is just one full-bleed image, e.g. copied verbatim from another deck.
// `logo: true` adds the white corner logo (verbatim slides already carry their own).
defineProps<{ image: string, logo?: boolean }>()
</script>

<template>
  <div class="slidev-layout sc picture">
    <img class="bleed" :src="image" alt="" />
    <slot />
    <Logo v-if="logo" light />
  </div>
</template>
