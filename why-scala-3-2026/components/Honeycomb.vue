<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  items: string[]
  highlight?: number
}>(), { highlight: -1 })

// Pointy-top hexagons in rows of 3, 4, 3 (fits 10 labels).
const W = 95 // half width
const R = 110 // radius
const rows = [
  { cy: 110, xs: [200, 400, 600] },
  { cy: 283, xs: [100, 300, 500, 700] },
  { cy: 456, xs: [200, 400, 600] },
]

const cells = computed(() => {
  const out: { cx: number, cy: number, points: string }[] = []
  for (const row of rows) {
    for (const cx of row.xs) {
      const cy = row.cy
      const pts = [
        [cx, cy - R], [cx + W, cy - R / 2], [cx + W, cy + R / 2],
        [cx, cy + R], [cx - W, cy + R / 2], [cx - W, cy - R / 2],
      ]
      out.push({ cx, cy, points: pts.map(p => p.join(',')).join(' ') })
    }
  }
  return out.slice(0, props.items.length)
})
</script>

<template>
  <svg class="honeycomb" viewBox="0 0 800 570" style="width: 100%; height: auto; display: block" role="img" :aria-label="items.join(', ')">
    <g v-for="(c, i) in cells" :key="i">
      <polygon :points="c.points" :style="{ fill: i === highlight ? 'var(--sc-accent)' : 'var(--sc-code-bg)' }" />
      <text
        :x="c.cx" :y="c.cy" text-anchor="middle" dominant-baseline="central"
        style="fill: var(--sc-light); font-size: 30px; font-weight: 600; font-family: var(--sc-display)"
      >{{ items[i] }}</text>
    </g>
  </svg>
</template>
