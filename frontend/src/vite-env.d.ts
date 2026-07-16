/// <reference types="vite/client" />

// Typing for the environment variables this application reads. Only non-secret, client-safe values
// belong here (Vite exposes VITE_* to the browser).
interface ImportMetaEnv {
  /** Base URL of the backend API (its /api/v1 root). Optional; falls back to a same-origin path. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
