<script setup lang="ts">
defineProps<{ light?: boolean }>()
</script>

<template>
  <img class="sxm-logo" :src="light ? '/logos/siriusxm-white.png' : '/logos/siriusxm-black.png'" alt="SiriusXM" />
</template>
