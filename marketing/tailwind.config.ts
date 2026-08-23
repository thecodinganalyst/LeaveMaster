import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/pages/**/*.{js,ts,jsx,tsx,mdx}', './src/components/**/*.{js,ts,jsx,tsx,mdx}', './src/app/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#e9f6f4',
          100: '#d0ece8',
          500: '#2d9c8f',
          600: '#23877c',
          700: '#1b6e66',
          900: '#103f3b'
        },
        slate: {
          50: '#f4f7fb',
          100: '#eaf2ff',
          200: '#dce6ef',
          300: '#c7d6e5',
          400: '#8fa5bb',
          500: '#667f99',
          600: '#4f6478',
          700: '#334e68',
          800: '#1c4b73',
          900: '#112f4d',
          950: '#0f2740'
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
