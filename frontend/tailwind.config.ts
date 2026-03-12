import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#132031',
        accent: '#1d4ed8',
        sand: '#f5f8ff'
      }
    },
  },
  plugins: [],
} satisfies Config
