// Testing foundation (TESTING_STRATEGY §8). Registers jest-dom matchers with Vitest's expect so tests
// can assert on user-observable DOM behavior, and unmounts each test's rendered tree afterward.
import "@testing-library/jest-dom/vitest";

import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// With Vitest globals disabled, Testing Library's automatic per-test cleanup is not registered, so we
// register it explicitly. Without this, DOM from one test leaks into the next and shared queries
// (e.g. by label) match multiple stale elements.
afterEach(() => {
  cleanup();
});
