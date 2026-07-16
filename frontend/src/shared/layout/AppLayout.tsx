import type { ReactNode } from "react";

/**
 * The application shell (FRONTEND_CODING_STANDARDS §4 — Layout).
 *
 * A generic, presentational structural container that wraps routed content in a page landmark. It
 * holds no navigation, feature logic, or data fetching; feature slices compose their pages inside it.
 * The {@code main} landmark is provided so routed content is announced correctly to assistive
 * technology by default.
 */
export function AppLayout({ children }: { children: ReactNode }) {
  return <main>{children}</main>;
}
