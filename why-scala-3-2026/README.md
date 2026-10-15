# Why did we choose Scala 3?

Slidev project for the talk. The theme is part of this project (no separate theme package):

```
slides.md            the talk (speaker notes are the <!-- --> comments)
styles/index.css     colours, type, all the layout classes
layouts/             cover · section · default · accent · statement · end · picture
components/          Card · BlockArrow · Honeycomb · Logo (SiriusXM corner logo)
setup/shiki.ts       code highlighting colours
```

## Run it

```bash
npm install
npm run dev        # http://localhost:3030, presenter view at /presenter
npm run build      # static site in dist/
npm run export     # PDF (needs playwright-chromium, already in devDependencies)
```

Fonts (Space Grotesk, IBM Plex Sans, JetBrains Mono) are bundled from npm, so the talk works offline.

## Writing slides

The canvas is 1920×1080, so sizes in CSS are real slide pixels.

| Layout      | Frontmatter                         | Use for                                  |
|-------------|-------------------------------------|------------------------------------------|
| `cover`     | `layout: cover`                     | Title slide (`# title` + subtitle line)  |
| `section`   | `layout: section`, `number: '02'`   | Section dividers                         |
| *(default)* | `eyebrow: Some label`               | Content slides; `# Title` first          |
| `accent`    | `layout: accent`, `eyebrow: …`      | Red statement: `## question` + `# punch` |
| `statement` | `layout: statement`                 | Dark quote: lead-in, `# big line`, kicker |
| `end`       | `layout: end`                       | Closing slide                            |
| `picture`   | `layout: picture`, `image: /x.png`  | Full-bleed image (slides copied verbatim) |

Add `class: spread` to a content slide to pin the last block to the bottom and centre the middle.

Building blocks inside slides:

- `<div class="cards">` with `<Card title="…" number="1" dark compact>` children
- `<div class="flow">` → cards joined with `<BlockArrow />`
- `<div class="tiles">` → 2×2 grid of `<div class="tile">`
- `<div class="split">` → `.main` (wide) + `.side` (narrow); `.stack` for a dark list card
- `<div class="compare">` → label | card → card grid (see the AI slide)
- Text: `p.lead`, `p.muted`, `p.punch` (red), `p.big`
- Code size: wrap a fenced block in `<div class="code-sm">` or `<div class="code-lg">`
