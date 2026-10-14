<script setup lang="ts">
import { ref } from 'vue'
import { onSlideEnter, onSlideLeave } from '@slidev/client'

const props = defineProps<{ src: string, poster: string, alt?: string }>()

// Slides stay mounted, so a plain <img> GIF keeps playing in the background.
// Off-slide we show a still of the first frame; on entering, a fresh URL makes
// the browser restart the GIF, and the still covers the gap while it loads.
const current = ref(props.poster)

onSlideEnter(() => {
  current.value = `${props.src}?t=${Date.now()}`
})

onSlideLeave(() => {
  current.value = props.poster
})
</script>

<template>
  <img :src="current" :alt="alt ?? ''">
</template>
