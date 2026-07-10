import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx,mdx}"],
  theme: {
    container: {
      center: true,
      padding: { DEFAULT: "1.5rem", lg: "2.5rem" },
      screens: { "2xl": "1440px" },
    },
    extend: {
      colors: {
        ink: "#0A0A0A",
        ivory: "#F5F1EA",
        gold: {
          DEFAULT: "#B8893E",
          soft: "#D4A85C",
          deep: "#8C6628",
        },
        stone: {
          50: "#FAF7F2",
          100: "#F1ECE3",
          200: "#DDD4C5",
          300: "#B8AC97",
          500: "#7A6F5C",
          700: "#3F392E",
        },
      },
      fontFamily: {
        display: ["var(--font-playfair)", "serif"],
        sans: ["var(--font-inter)", "system-ui", "sans-serif"],
      },
      letterSpacing: {
        tightest: "-0.04em",
        wider: "0.12em",
        widest: "0.22em",
      },
      borderRadius: {
        sm: "8px",
        md: "12px",
        lg: "20px",
      },
      spacing: {
        18: "4.5rem",
        22: "5.5rem",
        30: "7.5rem",
      },
      transitionTimingFunction: {
        editorial: "cubic-bezier(0.22, 1, 0.36, 1)",
      },
      transitionDuration: {
        "1200": "1200ms",
        "1400": "1400ms",
      },
      keyframes: {
        marquee: {
          "0%": { transform: "translateX(0)" },
          "100%": { transform: "translateX(-50%)" },
        },
      },
      animation: {
        marquee: "marquee 40s linear infinite",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
};

export default config;
