import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// Vite build + dev configuration (approved build tool, ADR-007). The test block configures Vitest as
// the Vite-native test runner for the testing foundation (TESTING_STRATEGY §8).
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: false,
    setupFiles: ["./src/test/setup.ts"],
  },
});
