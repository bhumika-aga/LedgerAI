import type { ErrorInfo, ReactNode } from "react";
import { Component } from "react";

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

/**
 * Global error boundary (FRONTEND_CODING_STANDARDS §10 — Global errors).
 *
 * The last-resort boundary that keeps a render failure from taking down the whole app. The fallback
 * is a neutral placeholder, not product messaging or a recovery flow; the design-system error UI and
 * centralized error reporting are wired by later slices.
 */
export class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    // Placeholder: centralized error reporting is introduced by a later slice.
    console.error(error, info);
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return <p role="alert">Something went wrong.</p>;
    }
    return this.props.children;
  }
}
