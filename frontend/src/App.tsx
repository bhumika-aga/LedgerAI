import { AppProviders } from "./app/AppProviders";
import { AppRouter } from "./app/AppRouter";

/**
 * Composition root of the LedgerAI frontend.
 *
 * It wires the cross-cutting providers around the router and nothing else. Feature slices attach
 * their routes and surfaces through the composition points below; this component holds no product
 * behavior.
 */
export default function App() {
  return (
    <AppProviders>
      <AppRouter />
    </AppProviders>
  );
}
