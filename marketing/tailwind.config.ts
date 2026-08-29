import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/pages/**/*.{js,ts,jsx,tsx,mdx}', './src/components/**/*.{js,ts,jsx,tsx,mdx}', './src/app/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#e9f6f4',
          100: '#d0ece8',
          200: '#a7ddd6',
          300: '#78c9bf',
          400: '#4bb3a7',
          500: '#2d9c8f',
          600: '#23877c',
          700: '#1b6e66',
          800: '#175852',
          900: '#103f3b',
          950: '#092b28'
        },
        slate: {
          50: '#fafafa',
          100: '#f4f4f5',
          200: '#e4e4e7',
          300: '#d4d4d8',
          400: '#a1a1aa',
          500: '#71717a',
          600: '#52525b',
          700: '#3f3f46',
          800: '#27272a',
          900: '#18181b',
          950: '#09090b'
        }
      },
      boxShadow: {
        soft: '0 20px 45px -20px rgba(45, 156, 143, 0.35)'
      }
    }
  },
  plugins: []
};

export default config;
