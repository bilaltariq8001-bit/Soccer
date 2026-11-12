import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["test/emulator/**/*.test.ts"],
    // Tests share one emulator database, so run files one after another.
    fileParallelism: false,
    testTimeout: 30_000,
    hookTimeout: 30_000,
  },
});
