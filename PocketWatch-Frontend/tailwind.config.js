/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['class'],
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Base surfaces — Dark Luxury
        canvas: '#09090B',        // page background
        surface: '#111827',       // sidebar / structural surface
        card: {
          DEFAULT: '#18181B',     // card background
          hover: '#1C1C20',
        },
        elevated: '#232326',      // inputs, dropdowns, secondary surface (~#27272A)

        // Foreground
        fg: {
          DEFAULT: '#FFFFFF',
          muted: '#FFFFFF',
          subtle: '#FFFFFF',
        },

        // Legacy token kept for text-paper/* (light-on-dark) — same family as fg
        paper: {
          DEFAULT: '#FFFFFF',
          dim: '#FFFFFF',
        },
        // Legacy token — dark surfaces (sidebar/hero/overlays/solid buttons)
        ink: {
          DEFAULT: '#09090B',
          light: '#1C1C20',
        },

        // Accent — warm gold
        brass: {
          DEFAULT: '#F5B942',
          light: '#F7C766',
          dark: '#C4901F',
        },
        gold: {
          DEFAULT: '#F5B942',
          light: '#F7C766',
          dark: '#C4901F',
        },

        // Semantic
        credit: {
          DEFAULT: '#22C55E',
          light: 'rgba(34, 197, 94, 0.14)',
        },
        debit: {
          DEFAULT: '#EF4444',
          light: 'rgba(239, 68, 68, 0.14)',
        },
        info: {
          DEFAULT: '#3B82F6',
          light: 'rgba(59, 130, 246, 0.14)',
        },
      },
      fontFamily: {
        display: ['"Plus Jakarta Sans"', 'sans-serif'],
        sans: ['"Plus Jakarta Sans"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace'],
      },
      borderRadius: {
        card: '16px',
        control: '10px',
      },
      boxShadow: {
        card: '0 1px 0 rgba(255,255,255,0.04) inset, 0 12px 32px -16px rgba(0,0,0,0.6)',
        glow: '0 0 0 1px rgba(245,185,66,0.15), 0 8px 30px -8px rgba(245,185,66,0.25)',
        pop: '0 20px 50px -12px rgba(0,0,0,0.7)',
      },
      backgroundImage: {
        'gold-gradient': 'linear-gradient(135deg, #F7C766 0%, #F5B942 45%, #C4901F 100%)',
        'card-sheen': 'linear-gradient(180deg, rgba(255,255,255,0.05) 0%, rgba(255,255,255,0) 100%)',
      },
    },
  },
  plugins: [],
}
