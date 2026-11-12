import { defineConfig } from "vitest/config";

// Fast unit tests only. Emulator tests run with `npm run test:emulated` (see vitest.emu.config.ts).
export default defineConfig({
  test: { include: ["test/*.test.ts"], exclude: ["test/emulator/**", "node_modules/**"] },
});
