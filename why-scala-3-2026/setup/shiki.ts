// Code highlighting theme matching the deck: dark panel, orange keywords,
// blue types, grey comments.
const scarlet = {
  name: 'scarlet',
  type: 'dark',
  colors: {
    'editor.background': '#1F2229',
    'editor.foreground': '#E6E1D6',
  },
  tokenColors: [
    { settings: { foreground: '#E6E1D6' } },
    {
      scope: ['comment', 'punctuation.definition.comment'],
      settings: { foreground: '#8A909C', fontStyle: 'italic' },
    },
    {
      scope: ['keyword', 'storage', 'keyword.control', 'keyword.declaration', 'constant.language'],
      settings: { foreground: '#FF9E64' },
    },
    {
      scope: [
        'entity.name.type',
        'entity.name.class',
        'entity.other.inherited-class',
        'support.type',
        'support.class',
        'storage.type.primitive',
      ],
      settings: { foreground: '#7AB8F0' },
    },
    {
      scope: ['string', 'string.quoted', 'markup.inline.raw'],
      settings: { foreground: '#B5D99C' },
    },
    {
      scope: ['constant.numeric'],
      settings: { foreground: '#F2C572' },
    },
    {
      scope: ['keyword.operator', 'punctuation'],
      settings: { foreground: '#E6E1D6' },
    },
  ],
}

export default () => ({
  themes: {
    dark: scarlet,
    light: scarlet,
  },
})
