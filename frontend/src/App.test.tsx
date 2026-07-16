import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import App from "./App";

/**
 * Scaffold smoke test (analogous to the backend context-load test). It proves the testing foundation
 * works and that the application composition — providers, router, and shell — mounts without error.
 * It asserts no product behavior.
 */
describe("App scaffold", () => {
  it("mounts the application shell", () => {
    render(<App />);
    expect(screen.getByRole("main")).toBeInTheDocument();
  });
});
