/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: { 50:'#eff6ff', 100:'#dbeafe', 500:'#3b82f6', 600:'#2563eb', 700:'#1d4ed8', 900:'#1e3a8a' },
        secondary: { 500:'#6366f1', 600:'#4f46e5' },
        success: { 500:'#10b981', 600:'#059669' },
        warning: { 500:'#f59e0b' },
        danger: { 500:'#ef4444', 600:'#dc2626' },
        dark: { 800:'#1e293b', 850:'#111827', 900:'#0b0f1a', 950:'#060c18' },
        surface: { DEFAULT:'#111827', 2:'#1a2235' },
      }
    },
  },
  // plugins: [
  //   require('@tailwindcss/forms'),
  // ],
}
