import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import App from "./App";

// Application entry point. Mounts the composition root; it contains no product behavior.
const rootElement = document.getElementById("root");
if (!rootElement) {
  throw new Error("Root element #root not found");
}

createRoot(rootElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
