/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#111111',
        paper: '#fafafa',
        muted: '#666666',
        line: '#e5e5e5',
      },
    },
  },
  plugins: [],
};
